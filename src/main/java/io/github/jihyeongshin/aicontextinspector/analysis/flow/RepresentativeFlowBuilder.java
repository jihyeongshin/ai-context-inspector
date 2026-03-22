package io.github.jihyeongshin.aicontextinspector.analysis.flow;

import io.github.jihyeongshin.aicontextinspector.model.source.ContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlow;

import java.util.*;

public class RepresentativeFlowBuilder {
    private static final int MAX_FLOW_DEPTH = 5;
    private static final Set<String> MAIN_FLOW_AFFINITIES = Set.of(
            "ApplicationLike",
            "PersistenceLike",
            "AdapterLike"
    );
    private static final Set<String> UNKNOWN_AFFINITY_ALLOWED_ROLES = Set.of(
            "Facade",
            "UseCase",
            "Service",
            "Repository",
            "Mapper",
            "Client",
            "Adapter"
    );
    private static final Set<String> TERMINAL_ROLES = Set.of(
            "Repository",
            "Mapper",
            "Client",
            "Adapter"
    );

    public List<RepresentativeFlow> build(ProjectContextSnapshot projectSnapshot) {
        if (projectSnapshot == null || projectSnapshot.isEmpty()) {
            return List.of();
        }

        List<ContextSnapshot> files = projectSnapshot.files();
        Map<String, List<ContextSnapshot>> classesByName = indexByClassName(files);
        List<ContextSnapshot> starts = findFlowStarts(files);
        Map<String, RepresentativeFlow> deduplicated = new LinkedHashMap<>();

        for (ContextSnapshot start : starts) {
            RepresentativeFlow flow = buildFlow(start, classesByName);
            if (flow == null || flow.classNames().size() < 2) {
                continue;
            }

            String key = flow.toDisplayString();
            RepresentativeFlow existing = deduplicated.get(key);
            if (existing == null || flow.score() > existing.score()) {
                deduplicated.put(key, flow);
            }
        }

        return deduplicated.values().stream()
                .sorted(Comparator
                        .comparingInt(RepresentativeFlow::score)
                        .reversed()
                        .thenComparing(RepresentativeFlow::toDisplayString))
                .toList();
    }

    private Map<String, List<ContextSnapshot>> indexByClassName(List<ContextSnapshot> files) {
        Map<String, List<ContextSnapshot>> index = new LinkedHashMap<>();
        for (ContextSnapshot file : files) {
            String className = normalize(file.className());
            if ("Unknown".equals(className)) {
                continue;
            }
            index.computeIfAbsent(className, key -> new ArrayList<>()).add(file);
        }
        return index;
    }

    private List<ContextSnapshot> findFlowStarts(List<ContextSnapshot> files) {
        List<ContextSnapshot> entryPoints = files.stream()
                .filter(this::isEntryPointLike)
                .sorted(Comparator
                        .comparing(ContextSnapshot::className, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ContextSnapshot::filePath, Comparator.nullsLast(String::compareTo)))
                .toList();

        if (!entryPoints.isEmpty()) {
            return entryPoints;
        }

        return files.stream()
                .filter(this::isApplicationLikeStart)
                .sorted(Comparator
                        .comparing(ContextSnapshot::className, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ContextSnapshot::filePath, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private RepresentativeFlow buildFlow(ContextSnapshot start, Map<String, List<ContextSnapshot>> classesByName) {
        List<String> classNames = new ArrayList<>();
        List<String> classRoles = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();

        classNames.add(normalize(start.className()));
        classRoles.add(normalize(start.classRole()));
        visited.add(snapshotKey(start));

        ContextSnapshot current = start;
        int totalScore = baseScore(start);

        while (classNames.size() < MAX_FLOW_DEPTH) {
            ContextSnapshot next = pickBestNext(current, classesByName, visited);
            if (next == null) {
                break;
            }

            classNames.add(normalize(next.className()));
            classRoles.add(normalize(next.classRole()));
            visited.add(snapshotKey(next));
            totalScore += transitionScore(current, next);

            if (isTerminal(next)) {
                break;
            }

            current = next;
        }

        if (classNames.size() < 2) {
            return null;
        }

        return new RepresentativeFlow(classNames, classRoles, totalScore);
    }

    private ContextSnapshot pickBestNext(
            ContextSnapshot current,
            Map<String, List<ContextSnapshot>> classesByName,
            Set<String> visited
    ) {
        ContextSnapshot best = null;
        int bestScore = Integer.MIN_VALUE;

        for (String dependencyName : safeList(current.dependencies())) {
            for (ContextSnapshot candidate : resolveDependencyCandidates(current, dependencyName, classesByName)) {
                if (visited.contains(snapshotKey(candidate))) {
                    continue;
                }
                if (!isRepresentativeCandidate(current, candidate)) {
                    continue;
                }

                int score = transitionScore(current, candidate);
                if (best == null || score > bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
        }

        return best;
    }

    private boolean isRepresentativeCandidate(ContextSnapshot current, ContextSnapshot candidate) {
        String candidateAffinity = normalize(candidate.architectureAffinity());
        String candidateRole = normalize(candidate.classRole());

        if (MAIN_FLOW_AFFINITIES.contains(candidateAffinity)) {
            return architectureTransitionScore(current, candidate) > 0;
        }

        if ("Unknown".equals(candidateAffinity) && UNKNOWN_AFFINITY_ALLOWED_ROLES.contains(candidateRole)) {
            return architectureTransitionScore(current, candidate) > 0;
        }

        if ("DomainLike".equals(candidateAffinity)
                || "DataLike".equals(candidateAffinity)
                || "SupportLike".equals(candidateAffinity)
                || "TestLike".equals(candidateAffinity)
                || "EntryPointLike".equals(candidateAffinity)) {
            return false;
        }

        return false;
    }

    private boolean isEntryPointLike(ContextSnapshot snapshot) {
        return "EntryPointLike".equals(normalize(snapshot.architectureAffinity()))
                || "Controller".equals(normalize(snapshot.classRole()))
                || !safeList(snapshot.endpoints()).isEmpty();
    }

    private boolean isApplicationLikeStart(ContextSnapshot snapshot) {
        String affinity = normalize(snapshot.architectureAffinity());
        String role = normalize(snapshot.classRole());
        return "ApplicationLike".equals(affinity)
                && ("Facade".equals(role) || "UseCase".equals(role) || "Service".equals(role));
    }

    private boolean isTerminal(ContextSnapshot snapshot) {
        String affinity = normalize(snapshot.architectureAffinity());
        String role = normalize(snapshot.classRole());
        return "PersistenceLike".equals(affinity)
                || "AdapterLike".equals(affinity)
                || TERMINAL_ROLES.contains(role);
    }

    private int baseScore(ContextSnapshot snapshot) {
        String affinity = normalize(snapshot.architectureAffinity());
        String role = normalize(snapshot.classRole());

        int score = 0;
        if ("EntryPointLike".equals(affinity)) {
            score += 40;
        } else if ("ApplicationLike".equals(affinity)) {
            score += 20;
        }

        if ("Controller".equals(role)) {
            score += 20;
        } else if ("Facade".equals(role) || "UseCase".equals(role) || "Service".equals(role)) {
            score += 10;
        }

        return score;
    }

    private int transitionScore(ContextSnapshot current, ContextSnapshot candidate) {
        return architectureTransitionScore(current, candidate)
                + nameAffinityScore(current.className(), candidate.className())
                + packageAffinityScore(current.packageName(), candidate.packageName());
    }

    private int architectureTransitionScore(ContextSnapshot current, ContextSnapshot candidate) {
        return roleTransitionPriority(normalize(current.classRole()), normalize(candidate.classRole()))
                + affinityTransitionPriority(
                normalize(current.architectureAffinity()),
                normalize(candidate.architectureAffinity())
        )
                + candidateRoleBonus(normalize(candidate.classRole()))
                + candidateAffinityBonus(normalize(candidate.architectureAffinity()));
    }

    private int roleTransitionPriority(String currentRole, String candidateRole) {
        if ("Controller".equals(currentRole)) {
            if ("Facade".equals(candidateRole)) return 70;
            if ("UseCase".equals(candidateRole)) return 65;
            if ("Service".equals(candidateRole)) return 60;
        }

        if ("Facade".equals(currentRole)) {
            if ("UseCase".equals(candidateRole)) return 60;
            if ("Service".equals(candidateRole)) return 55;
        }

        if ("UseCase".equals(currentRole)) {
            if ("Service".equals(candidateRole)) return 55;
            if ("Repository".equals(candidateRole) || "Mapper".equals(candidateRole)) return 50;
            if ("Client".equals(candidateRole) || "Adapter".equals(candidateRole)) return 40;
        }

        if ("Service".equals(currentRole)) {
            if ("Repository".equals(candidateRole) || "Mapper".equals(candidateRole)) return 60;
            if ("Client".equals(candidateRole) || "Adapter".equals(candidateRole)) return 50;
            if ("Service".equals(candidateRole)) return 25;
        }

        return 0;
    }

    private int affinityTransitionPriority(String currentAffinity, String candidateAffinity) {
        if ("EntryPointLike".equals(currentAffinity)) {
            if ("ApplicationLike".equals(candidateAffinity)) return 50;
            if ("PersistenceLike".equals(candidateAffinity)) return 20;
            if ("AdapterLike".equals(candidateAffinity)) return 10;
        }

        if ("ApplicationLike".equals(currentAffinity)) {
            if ("PersistenceLike".equals(candidateAffinity)) return 45;
            if ("AdapterLike".equals(candidateAffinity)) return 35;
            if ("ApplicationLike".equals(candidateAffinity)) return 20;
        }

        return 0;
    }

    private int candidateRoleBonus(String candidateRole) {
        if ("Facade".equals(candidateRole)) return 20;
        if ("UseCase".equals(candidateRole)) return 18;
        if ("Service".equals(candidateRole)) return 16;
        if ("Repository".equals(candidateRole)) return 14;
        if ("Mapper".equals(candidateRole)) return 13;
        if ("Client".equals(candidateRole) || "Adapter".equals(candidateRole)) return 12;
        return 0;
    }

    private int candidateAffinityBonus(String candidateAffinity) {
        if ("ApplicationLike".equals(candidateAffinity)) return 12;
        if ("PersistenceLike".equals(candidateAffinity)) return 10;
        if ("AdapterLike".equals(candidateAffinity)) return 8;
        return 0;
    }

    private int nameAffinityScore(String sourceClassName, String candidateClassName) {
        String sourceToken = normalizeDomainToken(sourceClassName);
        String candidateToken = normalizeDomainToken(candidateClassName);

        if (sourceToken.isBlank() || candidateToken.isBlank()) {
            return 0;
        }
        if (candidateToken.equals(sourceToken)) {
            return 6;
        }
        if (candidateToken.startsWith(sourceToken) || sourceToken.startsWith(candidateToken)) {
            return 3;
        }
        if (candidateToken.contains(sourceToken) || sourceToken.contains(candidateToken)) {
            return 1;
        }
        return 0;
    }

    private int packageAffinityScore(String sourcePackage, String candidatePackage) {
        return Math.min(commonPackageDepth(sourcePackage, candidatePackage), 3);
    }

    private List<ContextSnapshot> resolveDependencyCandidates(
            ContextSnapshot current,
            String dependencyName,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        List<ContextSnapshot> candidates = classesByName.getOrDefault(dependencyName, List.of());
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<ContextSnapshot> samePackage = new ArrayList<>();
        List<ContextSnapshot> samePackagePrefix = new ArrayList<>();
        List<ContextSnapshot> fallback = new ArrayList<>();

        for (ContextSnapshot candidate : candidates) {
            if (isSamePackage(current, candidate)) {
                samePackage.add(candidate);
            } else if (isSamePackagePrefix(current, candidate)) {
                samePackagePrefix.add(candidate);
            } else {
                fallback.add(candidate);
            }
        }

        if (!samePackage.isEmpty()) {
            return sortResolvedCandidates(current, samePackage);
        }
        if (!samePackagePrefix.isEmpty()) {
            return sortResolvedCandidates(current, samePackagePrefix);
        }
        return sortResolvedCandidates(current, fallback);
    }

    private List<ContextSnapshot> sortResolvedCandidates(ContextSnapshot current, List<ContextSnapshot> candidates) {
        return candidates.stream()
                .sorted(Comparator
                        .comparingInt((ContextSnapshot candidate) ->
                                commonPackageDepth(current.packageName(), candidate.packageName()))
                        .reversed()
                        .thenComparingInt(candidate ->
                                Math.abs(splitPackage(normalize(current.packageName())).size()
                                        - splitPackage(normalize(candidate.packageName())).size()))
                        .thenComparing(ContextSnapshot::className, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ContextSnapshot::filePath, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private boolean isSamePackage(ContextSnapshot current, ContextSnapshot candidate) {
        return normalize(current.packageName()).equals(normalize(candidate.packageName()));
    }

    private boolean isSamePackagePrefix(ContextSnapshot current, ContextSnapshot candidate) {
        String currentPackage = normalize(current.packageName());
        String candidatePackage = normalize(candidate.packageName());

        if ("Unknown".equals(currentPackage) || "Unknown".equals(candidatePackage)) {
            return false;
        }

        if (candidatePackage.startsWith(currentPackage + ".")) {
            return true;
        }

        String currentParent = parentPackage(currentPackage);
        return !currentParent.isBlank()
                && (candidatePackage.equals(currentParent) || candidatePackage.startsWith(currentParent + "."));
    }

    private String parentPackage(String packageName) {
        String normalized = normalize(packageName);
        if ("Unknown".equals(normalized)) {
            return "";
        }

        int lastDot = normalized.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return normalized.substring(0, lastDot);
    }

    private int commonPackageDepth(String sourcePackage, String candidatePackage) {
        List<String> sourceParts = splitPackage(sourcePackage);
        List<String> candidateParts = splitPackage(candidatePackage);
        int max = Math.min(sourceParts.size(), candidateParts.size());
        int common = 0;

        while (common < max && Objects.equals(sourceParts.get(common), candidateParts.get(common))) {
            common++;
        }

        return common;
    }

    private List<String> splitPackage(String packageName) {
        String normalized = normalize(packageName);
        if ("Unknown".equals(normalized)) {
            return List.of();
        }
        return List.of(normalized.split("\\."));
    }

    private String normalizeDomainToken(String className) {
        return normalize(className)
                .replace("Controller", "")
                .replace("Facade", "")
                .replace("UseCase", "")
                .replace("Service", "")
                .replace("Repository", "")
                .replace("Mapper", "")
                .replace("Client", "")
                .replace("Adapter", "")
                .replace("Dispatcher", "")
                .replace("Request", "")
                .replace("Response", "")
                .replace("Dto", "")
                .replace("DTO", "")
                .trim();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String snapshotKey(ContextSnapshot snapshot) {
        return normalize(snapshot.packageName()) + "." + normalize(snapshot.className());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }
}

