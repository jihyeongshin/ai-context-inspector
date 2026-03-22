package io.github.jihyeongshin.aicontextinspector.analysis.flow;

import io.github.jihyeongshin.aicontextinspector.model.flow.EntryPointInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.FlowAmbiguity;
import io.github.jihyeongshin.aicontextinspector.model.flow.FlowConfidence;
import io.github.jihyeongshin.aicontextinspector.model.flow.InterpretedRepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowEntryPointInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowMetadata;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.source.ContextSnapshot;


import java.util.*;

public class RepresentativeFlowMetadataEvaluator {
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
    private static final Set<String> COMPETITION_RELEVANT_ROLES = Set.of(
            "Facade",
            "UseCase",
            "Service",
            "Repository",
            "Mapper",
            "Client",
            "Adapter"
    );
    private static final int MULTI_PURPOSE_ENDPOINT_THRESHOLD = 6;
    private static final int MULTI_PURPOSE_DEPENDENCY_THRESHOLD = 10;
    private static final int MULTI_PURPOSE_REPRESENTATIVE_CANDIDATE_THRESHOLD = 4;
    private static final int LEGACY_DEPENDENCY_THRESHOLD = 10;
    private static final int LEGACY_APPLICATION_DEPENDENCY_THRESHOLD = 8;
    private static final int CLOSE_COMPETITION_GAP = 6;
    private static final int MIN_COMPETING_CANDIDATE_SCORE = 115;
    private static final int MAX_ARCHITECTURE_SCORE_GAP = 5;
    private static final int MIN_COMPETING_PACKAGE_AFFINITY = 2;
    private static final int MIN_COMPETING_NAME_AFFINITY = 3;
    private static final String NOTE_CANONICAL_LAYERED_CHAIN = "canonical layered chain";
    private static final String NOTE_WEAK_SERVICE_TO_SERVICE_HOP = "weak service-to-service hop";
    private static final String NOTE_MULTI_PURPOSE_ENTRY_POINT = "multi-purpose entry point";
    private static final String NOTE_COMPETING_REPRESENTATIVE_CANDIDATES = "competing representative candidates";
    private static final String NOTE_LEGACY_HEAVY_ORCHESTRATION_ZONE = "legacy-heavy orchestration zone";

    private final RepresentativeFlowEntryPointInterpreter representativeFlowEntryPointInterpreter =
            new RepresentativeFlowEntryPointInterpreter();

    public List<InterpretedRepresentativeFlow> evaluate(
            ProjectContextSnapshot snapshot,
            List<RepresentativeFlow> flows
    ) {
        if (snapshot == null || snapshot.isEmpty() || flows == null || flows.isEmpty()) {
            return List.of();
        }

        Map<String, List<ContextSnapshot>> classesByName = indexByClassName(snapshot.files());
        Map<String, RepresentativeFlowEntryPointInterpretation> entryPointInterpretations =
                representativeFlowEntryPointInterpreter.evaluate(snapshot, flows);
        Map<String, Long> startRoleCounts = countBoundaryRoles(flows, true);
        Map<String, Long> terminalRoleCounts = countBoundaryRoles(flows, false);
        Map<String, Long> transitionCounts = countRoleTransitions(flows);

        List<InterpretedRepresentativeFlow> interpretedFlows = new ArrayList<>();
        for (RepresentativeFlow flow : flows) {
            interpretedFlows.add(new InterpretedRepresentativeFlow(
                    flow,
                    evaluateFlow(
                            snapshot,
                            flow,
                            flows.size(),
                            classesByName,
                            entryPointInterpretations,
                            startRoleCounts,
                            terminalRoleCounts,
                            transitionCounts
                    )
            ));
        }
        return List.copyOf(interpretedFlows);
    }

    private RepresentativeFlowMetadata evaluateFlow(
            ProjectContextSnapshot snapshot,
            RepresentativeFlow flow,
            int totalFlows,
            Map<String, List<ContextSnapshot>> classesByName,
            Map<String, RepresentativeFlowEntryPointInterpretation> entryPointInterpretations,
            Map<String, Long> startRoleCounts,
            Map<String, Long> terminalRoleCounts,
            Map<String, Long> transitionCounts
    ) {
        List<String> classNames = safeList(flow.classNames());
        List<String> classRoles = safeList(flow.classRoles());
        List<ContextSnapshot> resolvedNodes = resolveFlowNodes(classNames, classRoles, classesByName);
        List<String> transitions = buildTransitions(classRoles);

        boolean startStable = isBoundaryRoleStable(classRoles, startRoleCounts, totalFlows, true);
        boolean terminalStable = isBoundaryRoleStable(classRoles, terminalRoleCounts, totalFlows, false);
        double transitionTypicality = computeTransitionTypicality(transitions, transitionCounts);
        boolean serviceToServiceHop = hasServiceToServiceHop(classRoles);
        // Entry-point interpretation owns the multi-purpose signal so preview and metadata stay aligned.
        boolean multiPurposeEntryPoint = isMultiPurposeEntryPoint(snapshot, flow, entryPointInterpretations);
        boolean legacyHotspot = isLegacyHotspot(resolvedNodes, classRoles, serviceToServiceHop);
        boolean competingCandidates = hasCompetingRepresentativeCandidates(resolvedNodes, classesByName);
        boolean canonicalLength = classNames.size() >= 3 && classNames.size() <= 5;
        boolean canonicalRolePattern = isCanonicalRolePattern(classRoles);
        boolean canonicalShape = canonicalRolePattern
                && transitionTypicality >= 0.6
                && !serviceToServiceHop;

        int confidenceScore = 0;
        if (canonicalShape) {
            confidenceScore += 3;
        } else if (canonicalLength) {
            confidenceScore += 1;
        } else if (classNames.size() <= 2 || classNames.size() >= 6) {
            confidenceScore -= 1;
        }
        if (startStable) {
            confidenceScore += 1;
        }
        if (terminalStable) {
            confidenceScore += 1;
        }
        if (transitionTypicality >= 0.75) {
            confidenceScore += 2;
        } else if (transitionTypicality >= 0.4) {
            confidenceScore += 1;
        } else {
            confidenceScore -= 1;
        }
        if (competingCandidates) {
            confidenceScore -= 2;
        }
        if (serviceToServiceHop) {
            confidenceScore -= 2;
        }
        if (multiPurposeEntryPoint && !canonicalShape) {
            confidenceScore -= 1;
        }
        if (legacyHotspot && (serviceToServiceHop || !canonicalLength)) {
            confidenceScore -= 1;
        }

        int ambiguityScore = 0;
        if (competingCandidates) {
            ambiguityScore += 2;
        }
        if (multiPurposeEntryPoint && competingCandidates) {
            ambiguityScore += 1;
        }
        if (!startStable && !terminalStable && transitionTypicality < 0.4) {
            ambiguityScore += 1;
        }

        List<String> notes = new ArrayList<>();
        if (canonicalShape) {
            notes.add(NOTE_CANONICAL_LAYERED_CHAIN);
        }
        if (serviceToServiceHop) {
            notes.add(NOTE_WEAK_SERVICE_TO_SERVICE_HOP);
        }
        if (multiPurposeEntryPoint) {
            notes.add(NOTE_MULTI_PURPOSE_ENTRY_POINT);
        }
        if (competingCandidates) {
            notes.add(NOTE_COMPETING_REPRESENTATIVE_CANDIDATES);
        }
        if (legacyHotspot) {
            notes.add(NOTE_LEGACY_HEAVY_ORCHESTRATION_ZONE);
        }

        return new RepresentativeFlowMetadata(
                classifyConfidence(confidenceScore),
                classifyAmbiguity(ambiguityScore),
                notes
        );
    }

    private FlowConfidence classifyConfidence(int confidenceScore) {
        if (confidenceScore >= 6) {
            return FlowConfidence.HIGH;
        }
        if (confidenceScore <= 1) {
            return FlowConfidence.LOW;
        }
        return FlowConfidence.MEDIUM;
    }

    private FlowAmbiguity classifyAmbiguity(int ambiguityScore) {
        if (ambiguityScore >= 4) {
            return FlowAmbiguity.HIGH;
        }
        if (ambiguityScore >= 1) {
            return FlowAmbiguity.POSSIBLE;
        }
        return FlowAmbiguity.NONE;
    }

    private Map<String, List<ContextSnapshot>> indexByClassName(List<ContextSnapshot> files) {
        Map<String, List<ContextSnapshot>> index = new LinkedHashMap<>();
        for (ContextSnapshot file : safeContextList(files)) {
            String className = normalize(file.className());
            if ("Unknown".equals(className)) {
                continue;
            }
            index.computeIfAbsent(className, key -> new ArrayList<>()).add(file);
        }
        return index;
    }

    private Map<String, Long> countBoundaryRoles(List<RepresentativeFlow> flows, boolean startBoundary) {
        Map<String, Long> counts = new TreeMap<>();
        for (RepresentativeFlow flow : safeFlowList(flows)) {
            List<String> roles = safeList(flow.classRoles());
            if (roles.isEmpty()) {
                continue;
            }
            String role = startBoundary ? normalize(roles.get(0)) : normalize(roles.get(roles.size() - 1));
            counts.put(role, counts.getOrDefault(role, 0L) + 1);
        }
        return counts;
    }

    private Map<String, Long> countRoleTransitions(List<RepresentativeFlow> flows) {
        Map<String, Long> counts = new TreeMap<>();
        for (RepresentativeFlow flow : safeFlowList(flows)) {
            List<String> roles = safeList(flow.classRoles());
            for (int index = 0; index < roles.size() - 1; index++) {
                String transition = normalize(roles.get(index)) + " -> " + normalize(roles.get(index + 1));
                counts.put(transition, counts.getOrDefault(transition, 0L) + 1);
            }
        }
        return counts;
    }

    private List<String> buildTransitions(List<String> classRoles) {
        List<String> transitions = new ArrayList<>();
        for (int index = 0; index < classRoles.size() - 1; index++) {
            transitions.add(normalize(classRoles.get(index)) + " -> " + normalize(classRoles.get(index + 1)));
        }
        return transitions;
    }

    private boolean isBoundaryRoleStable(
            List<String> classRoles,
            Map<String, Long> boundaryCounts,
            int totalFlows,
            boolean startBoundary
    ) {
        if (classRoles.isEmpty() || totalFlows < 2 || boundaryCounts.isEmpty()) {
            return false;
        }

        String role = startBoundary ? normalize(classRoles.get(0)) : normalize(classRoles.get(classRoles.size() - 1));
        long roleCount = boundaryCounts.getOrDefault(role, 0L);
        long dominantCount = boundaryCounts.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        return roleCount >= 2
                && roleCount == dominantCount
                && (roleCount * 2.0) >= totalFlows;
    }

    private double computeTransitionTypicality(List<String> transitions, Map<String, Long> transitionCounts) {
        if (transitions.isEmpty()) {
            return 0.0;
        }

        int typicalCount = 0;
        for (String transition : transitions) {
            long count = transitionCounts.getOrDefault(transition, 0L);
            if (count >= 2 || isCanonicalTransition(transition)) {
                typicalCount++;
            }
        }
        return typicalCount / (double) transitions.size();
    }

    private boolean hasServiceToServiceHop(List<String> classRoles) {
        for (int index = 0; index < classRoles.size() - 1; index++) {
            if ("Service".equals(normalize(classRoles.get(index)))
                    && "Service".equals(normalize(classRoles.get(index + 1)))) {
                return true;
            }
        }
        return false;
    }

    private boolean isMultiPurposeEntryPoint(
            ProjectContextSnapshot snapshot,
            RepresentativeFlow flow,
            Map<String, RepresentativeFlowEntryPointInterpretation> entryPointInterpretations
    ) {
        String identity = representativeFlowEntryPointInterpreter.entryPointIdentity(snapshot, flow);
        RepresentativeFlowEntryPointInterpretation interpretation = entryPointInterpretations.get(identity);
        return interpretation != null
                && interpretation.interpretation() == EntryPointInterpretation.MULTI_PURPOSE;
    }

    private boolean isLegacyHotspot(
            List<ContextSnapshot> resolvedNodes,
            List<String> classRoles,
            boolean serviceToServiceHop
    ) {
        for (int index = 0; index < resolvedNodes.size(); index++) {
            ContextSnapshot node = resolvedNodes.get(index);
            int dependencyCount = safeList(node.dependencies()).size();
            String role = index < classRoles.size() ? normalize(classRoles.get(index)) : normalize(node.classRole());

            if (dependencyCount >= LEGACY_DEPENDENCY_THRESHOLD) {
                return true;
            }
            if (serviceToServiceHop
                    && ("Facade".equals(role) || "UseCase".equals(role) || "Service".equals(role))
                    && dependencyCount >= LEGACY_APPLICATION_DEPENDENCY_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCompetingRepresentativeCandidates(
            List<ContextSnapshot> resolvedNodes,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        Set<String> visited = new LinkedHashSet<>();

        for (int index = 0; index < resolvedNodes.size() - 1; index++) {
            ContextSnapshot current = resolvedNodes.get(index);
            visited.add(snapshotKey(current));

            List<CandidateScore> candidates = scoreRepresentativeCandidates(current, classesByName, visited);
            if (candidates.size() < 2) {
                continue;
            }

            CandidateScore best = candidates.get(0);
            CandidateScore runnerUp = candidates.get(1);
            boolean closeGap = (best.score() - runnerUp.score()) <= CLOSE_COMPETITION_GAP;
            if (closeGap && isPlausibleCompetition(best, runnerUp)) {
                return true;
            }
        }

        return false;
    }

    private List<CandidateScore> scoreRepresentativeCandidates(
            ContextSnapshot current,
            Map<String, List<ContextSnapshot>> classesByName,
            Set<String> visited
    ) {
        Map<String, CandidateScore> scores = new LinkedHashMap<>();
        for (String dependencyName : safeList(current.dependencies())) {
            for (ContextSnapshot candidate : resolveDependencyCandidates(current, dependencyName, classesByName)) {
                if (visited.contains(snapshotKey(candidate))) {
                    continue;
                }
                if (!isRepresentativeCandidate(current, candidate)) {
                    continue;
                }

                String role = normalize(candidate.classRole());
                if (!COMPETITION_RELEVANT_ROLES.contains(role)) {
                    continue;
                }

                CandidateScore candidateScore = buildCandidateScore(current, candidate);
                scores.merge(
                        snapshotKey(candidate),
                        candidateScore,
                        (existing, replacement) -> existing.score() >= replacement.score() ? existing : replacement
                );
            }
        }

        return scores.values().stream()
                .sorted(Comparator
                        .comparingInt(CandidateScore::score)
                        .reversed()
                        .thenComparing(candidateScore -> normalize(candidateScore.candidate().className()))
                        .thenComparing(candidateScore -> normalize(candidateScore.candidate().filePath())))
                .toList();
    }

    private int countRepresentativeFriendlyDependencies(
            ContextSnapshot current,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        Set<String> candidates = new LinkedHashSet<>();
        for (String dependencyName : safeList(current.dependencies())) {
            for (ContextSnapshot candidate : resolveDependencyCandidates(current, dependencyName, classesByName)) {
                if (isRepresentativeCandidate(current, candidate)) {
                    candidates.add(snapshotKey(candidate));
                }
            }
        }
        return candidates.size();
    }

    private CandidateScore buildCandidateScore(ContextSnapshot current, ContextSnapshot candidate) {
        int architectureScore = architectureTransitionScore(current, candidate);
        int nameScore = nameAffinityScore(current.className(), candidate.className());
        int packageScore = packageAffinityScore(current.packageName(), candidate.packageName());
        return new CandidateScore(
                candidate,
                architectureScore + nameScore + packageScore,
                architectureScore,
                packageScore,
                nameScore
        );
    }

    private boolean isPlausibleCompetition(CandidateScore best, CandidateScore runnerUp) {
        boolean runnerUpStrong = runnerUp.score() >= MIN_COMPETING_CANDIDATE_SCORE;
        boolean sameRoleFamily = normalize(best.candidate().classRole())
                .equals(normalize(runnerUp.candidate().classRole()));
        boolean sameAffinityFamily = normalize(best.candidate().architectureAffinity())
                .equals(normalize(runnerUp.candidate().architectureAffinity()));
        boolean similarArchitecture =
                Math.abs(best.architectureScore() - runnerUp.architectureScore()) <= MAX_ARCHITECTURE_SCORE_GAP;
        boolean similarPackageProximity = best.packageScore() >= MIN_COMPETING_PACKAGE_AFFINITY
                && runnerUp.packageScore() >= MIN_COMPETING_PACKAGE_AFFINITY;
        boolean similarNameAffinity = best.nameScore() >= MIN_COMPETING_NAME_AFFINITY
                && runnerUp.nameScore() >= MIN_COMPETING_NAME_AFFINITY;

        return runnerUpStrong
                && sameRoleFamily
                && sameAffinityFamily
                && similarArchitecture
                && similarPackageProximity
                && similarNameAffinity;
    }

    private List<ContextSnapshot> resolveFlowNodes(
            List<String> classNames,
            List<String> classRoles,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        List<ContextSnapshot> resolvedNodes = new ArrayList<>();
        ContextSnapshot previous = null;

        for (int index = 0; index < classNames.size(); index++) {
            String className = normalize(classNames.get(index));
            String classRole = index < classRoles.size() ? normalize(classRoles.get(index)) : "Unknown";
            ContextSnapshot resolved = resolveFlowNode(previous, className, classRole, classesByName);
            if (resolved != null) {
                resolvedNodes.add(resolved);
                previous = resolved;
            }
        }

        return resolvedNodes;
    }

    private ContextSnapshot resolveFlowNode(
            ContextSnapshot previous,
            String className,
            String classRole,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        List<ContextSnapshot> candidates = new ArrayList<>(classesByName.getOrDefault(className, List.of()));
        if (candidates.isEmpty()) {
            return null;
        }

        ContextSnapshot best = null;
        int bestRoleScore = Integer.MIN_VALUE;
        int bestResolutionScore = Integer.MIN_VALUE;

        for (ContextSnapshot candidate : candidates) {
            int roleScore = roleMatchScore(candidate, classRole);
            int resolutionScore = previous == null
                    ? entryPointResolutionScore(candidate)
                    : packageResolutionScore(previous, candidate);

            if (best == null
                    || roleScore > bestRoleScore
                    || (roleScore == bestRoleScore && resolutionScore > bestResolutionScore)
                    || (roleScore == bestRoleScore
                    && resolutionScore == bestResolutionScore
                    && compareNullable(candidate.filePath(), best.filePath()) < 0)) {
                best = candidate;
                bestRoleScore = roleScore;
                bestResolutionScore = resolutionScore;
            }
        }

        return best;
    }

    private int roleMatchScore(ContextSnapshot candidate, String classRole) {
        return normalize(candidate.classRole()).equals(classRole) ? 2 : 0;
    }

    private int entryPointResolutionScore(ContextSnapshot candidate) {
        int score = 0;
        if (isEntryPointLike(candidate)) {
            score += 2;
        }
        if ("Controller".equals(normalize(candidate.classRole()))) {
            score += 1;
        }
        return score;
    }

    private int packageResolutionScore(ContextSnapshot previous, ContextSnapshot candidate) {
        return commonPackageDepth(previous.packageName(), candidate.packageName());
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

    private boolean isRepresentativeCandidate(ContextSnapshot current, ContextSnapshot candidate) {
        String candidateAffinity = normalize(candidate.architectureAffinity());
        String candidateRole = normalize(candidate.classRole());

        if (MAIN_FLOW_AFFINITIES.contains(candidateAffinity)) {
            return architectureTransitionScore(current, candidate) > 0;
        }

        if ("Unknown".equals(candidateAffinity) && UNKNOWN_AFFINITY_ALLOWED_ROLES.contains(candidateRole)) {
            return architectureTransitionScore(current, candidate) > 0;
        }

        return false;
    }

    private boolean isEntryPointLike(ContextSnapshot snapshot) {
        return "EntryPointLike".equals(normalize(snapshot.architectureAffinity()))
                || "Controller".equals(normalize(snapshot.classRole()))
                || !safeList(snapshot.endpoints()).isEmpty();
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

    private boolean isCanonicalTransition(String transition) {
        return "Controller -> Facade".equals(transition)
                || "Controller -> UseCase".equals(transition)
                || "Controller -> Service".equals(transition)
                || "Facade -> UseCase".equals(transition)
                || "Facade -> Service".equals(transition)
                || "UseCase -> Service".equals(transition)
                || "UseCase -> Repository".equals(transition)
                || "UseCase -> Mapper".equals(transition)
                || "Service -> Repository".equals(transition)
                || "Service -> Mapper".equals(transition)
                || "Service -> Client".equals(transition)
                || "Service -> Adapter".equals(transition);
    }

    private boolean isCanonicalRolePattern(List<String> classRoles) {
        List<String> roles = safeList(classRoles).stream()
                .map(this::normalize)
                .toList();
        if (roles.size() == 3) {
            return "Controller".equals(roles.get(0))
                    && isApplicationRole(roles.get(1))
                    && isTerminalRole(roles.get(2));
        }
        if (roles.size() == 4) {
            return "Controller".equals(roles.get(0))
                    && "Facade".equals(roles.get(1))
                    && "UseCase".equals(roles.get(2))
                    && isTerminalRole(roles.get(3));
        }
        if (roles.size() == 5) {
            return "Controller".equals(roles.get(0))
                    && "Facade".equals(roles.get(1))
                    && "UseCase".equals(roles.get(2))
                    && "Service".equals(roles.get(3))
                    && isTerminalRole(roles.get(4));
        }
        return false;
    }

    private boolean isApplicationRole(String role) {
        return "Facade".equals(role)
                || "UseCase".equals(role)
                || "Service".equals(role);
    }

    private boolean isTerminalRole(String role) {
        return "Repository".equals(role)
                || "Mapper".equals(role)
                || "Client".equals(role)
                || "Adapter".equals(role);
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

    private List<ContextSnapshot> safeContextList(List<ContextSnapshot> values) {
        return values == null ? List.of() : values;
    }

    private List<RepresentativeFlow> safeFlowList(List<RepresentativeFlow> values) {
        return values == null ? List.of() : values;
    }

    private String snapshotKey(ContextSnapshot snapshot) {
        return normalize(snapshot.packageName()) + "." + normalize(snapshot.className());
    }

    private int compareNullable(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }

    private record CandidateScore(
            ContextSnapshot candidate,
            int score,
            int architectureScore,
            int packageScore,
            int nameScore
    ) {
    }
}
