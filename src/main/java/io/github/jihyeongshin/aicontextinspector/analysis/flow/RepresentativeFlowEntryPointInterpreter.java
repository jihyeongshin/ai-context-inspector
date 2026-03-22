package io.github.jihyeongshin.aicontextinspector.analysis.flow;

import io.github.jihyeongshin.aicontextinspector.model.flow.EntryPointInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowEntryPointInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.source.ContextSnapshot;


import java.util.*;

public class RepresentativeFlowEntryPointInterpreter {
    private static final int BROAD_ENDPOINT_THRESHOLD = 6;
    private static final int BROAD_DEPENDENCY_THRESHOLD = 6;
    private static final int MIXED_ROUTE_SIGNATURE_THRESHOLD = 3;
    private static final int NAVIGATION_ROUTE_THRESHOLD = 2;
    private static final Set<String> NAVIGATION_ROUTE_TOKENS = Set.of(
            "account",
            "error",
            "health",
            "home",
            "index",
            "intro",
            "login",
            "logout",
            "noop",
            "status"
    );
    private static final String NOTE_BROAD_ENDPOINT_SURFACE = "broad endpoint surface";
    private static final String NOTE_BROAD_DEPENDENCY_SURFACE = "broad dependency surface";
    private static final String NOTE_MIXED_ROUTING_PATTERN = "mixed routing pattern";
    private static final String NOTE_HETEROGENEOUS_DOWNSTREAM_TARGETS = "heterogeneous downstream targets";
    private static final String NOTE_NAVIGATION_HEAVY_ENTRY_POINT = "navigation-heavy entry point";

    public Map<String, RepresentativeFlowEntryPointInterpretation> evaluate(
            ProjectContextSnapshot snapshot,
            List<RepresentativeFlow> flows
    ) {
        if (snapshot == null || snapshot.isEmpty() || flows == null || flows.isEmpty()) {
            return Map.of();
        }

        Map<String, List<ContextSnapshot>> classesByName = indexByClassName(snapshot.files());
        Map<String, RepresentativeFlowEntryPointInterpretation> interpretations = new LinkedHashMap<>();
        Map<String, List<RepresentativeFlow>> flowsByIdentity = new LinkedHashMap<>();
        Map<String, ContextSnapshot> entryPointsByIdentity = new LinkedHashMap<>();

        for (RepresentativeFlow flow : safeFlowList(flows)) {
            ContextSnapshot entryPoint = resolveEntryPoint(flow, classesByName);
            if (entryPoint == null) {
                continue;
            }

            String identity = snapshotIdentity(entryPoint);
            entryPointsByIdentity.putIfAbsent(identity, entryPoint);
            flowsByIdentity.computeIfAbsent(identity, key -> new ArrayList<>()).add(flow);
        }

        for (Map.Entry<String, ContextSnapshot> entry : entryPointsByIdentity.entrySet()) {
            List<RepresentativeFlow> groupedFlows = flowsByIdentity.getOrDefault(entry.getKey(), List.of());
            interpretations.put(entry.getKey(), interpretEntryPoint(entry.getValue(), groupedFlows));
        }

        return Map.copyOf(interpretations);
    }

    public String entryPointIdentity(ProjectContextSnapshot snapshot, RepresentativeFlow flow) {
        if (snapshot == null || snapshot.isEmpty() || flow == null) {
            return "Unknown";
        }

        Map<String, List<ContextSnapshot>> classesByName = indexByClassName(snapshot.files());
        ContextSnapshot entryPoint = resolveEntryPoint(flow, classesByName);
        return snapshotIdentity(entryPoint);
    }

    private RepresentativeFlowEntryPointInterpretation interpretEntryPoint(
            ContextSnapshot entryPoint,
            List<RepresentativeFlow> flows
    ) {
        List<String> notes = new ArrayList<>();
        int endpointCount = safeList(entryPoint.endpoints()).size();
        int dependencyCount = safeList(entryPoint.dependencies()).size();
        RouteProfile routeProfile = buildRouteProfile(entryPoint);
        DownstreamProfile downstreamProfile = buildDownstreamProfile(flows);

        boolean broadEndpointSurface = endpointCount >= BROAD_ENDPOINT_THRESHOLD;
        boolean broadDependencySurface = dependencyCount >= BROAD_DEPENDENCY_THRESHOLD;
        boolean mixedRoutingPattern = routeProfile.distinctSignatures() >= MIXED_ROUTE_SIGNATURE_THRESHOLD
                || routeProfile.distinctFamilies() >= 2;
        boolean navigationHeavy = routeProfile.navigationHeavy();
        boolean heterogeneousDownstreamTargets = downstreamProfile.heterogeneousTargets();

        if (broadEndpointSurface) {
            notes.add(NOTE_BROAD_ENDPOINT_SURFACE);
        }
        if (broadDependencySurface) {
            notes.add(NOTE_BROAD_DEPENDENCY_SURFACE);
        }
        if (mixedRoutingPattern) {
            notes.add(NOTE_MIXED_ROUTING_PATTERN);
        }
        if (heterogeneousDownstreamTargets) {
            notes.add(NOTE_HETEROGENEOUS_DOWNSTREAM_TARGETS);
        }
        if (navigationHeavy) {
            notes.add(NOTE_NAVIGATION_HEAVY_ENTRY_POINT);
        }

        EntryPointInterpretation interpretation = isMultiPurpose(
                broadEndpointSurface,
                broadDependencySurface,
                mixedRoutingPattern,
                navigationHeavy,
                heterogeneousDownstreamTargets
        )
                ? EntryPointInterpretation.MULTI_PURPOSE
                : EntryPointInterpretation.FOCUSED;
        return interpretation == EntryPointInterpretation.MULTI_PURPOSE
                ? new RepresentativeFlowEntryPointInterpretation(interpretation, notes)
                : new RepresentativeFlowEntryPointInterpretation(interpretation, List.of());
    }

    private ContextSnapshot resolveEntryPoint(
            RepresentativeFlow flow,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        List<String> classNames = safeList(flow.classNames());
        List<String> classRoles = safeList(flow.classRoles());
        if (classNames.isEmpty()) {
            return null;
        }

        String className = normalize(classNames.get(0));
        List<ContextSnapshot> candidates = new ArrayList<>(classesByName.getOrDefault(className, List.of()));
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        String classRole = classRoles.isEmpty() ? "Unknown" : normalize(classRoles.get(0));
        List<ContextSnapshot> nextCandidates = classNames.size() > 1
                ? classesByName.getOrDefault(normalize(classNames.get(1)), List.of())
                : List.of();

        return candidates.stream()
                .max(Comparator
                        .comparingInt((ContextSnapshot candidate) -> roleMatchScore(candidate, classRole))
                        .thenComparingInt(this::entryPointStrengthScore)
                        .thenComparingInt(candidate -> nextNodePackageScore(candidate, nextCandidates))
                        .thenComparing(candidate -> normalize(candidate.filePath()), Comparator.reverseOrder()))
                .orElse(null);
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

    private int entryPointScore(ContextSnapshot candidate) {
        int score = 0;
        if (isEntryPointLike(candidate)) {
            score += 2;
        }
        if ("Controller".equals(normalize(candidate.classRole()))) {
            score += 1;
        }
        return score;
    }

    private int roleMatchScore(ContextSnapshot candidate, String classRole) {
        return normalize(candidate.classRole()).equals(classRole) ? 2 : 0;
    }

    private int entryPointStrengthScore(ContextSnapshot entryPoint) {
        RouteProfile routeProfile = buildRouteProfile(entryPoint);
        int score = entryPointScore(entryPoint);
        score += safeList(entryPoint.endpoints()).size();
        score += safeList(entryPoint.dependencies()).size();
        score += routeProfile.distinctSignatures();
        if (routeProfile.navigationHeavy()) {
            score += 2;
        }
        return score;
    }

    private int nextNodePackageScore(ContextSnapshot current, List<ContextSnapshot> nextCandidates) {
        int bestScore = 0;
        for (ContextSnapshot candidate : safeContextList(nextCandidates)) {
            bestScore = Math.max(bestScore, commonPackageDepth(current.packageName(), candidate.packageName()));
        }
        return bestScore;
    }

    private RouteProfile buildRouteProfile(ContextSnapshot entryPoint) {
        Set<String> routeFamilies = new LinkedHashSet<>();
        Set<String> routeSignatures = new LinkedHashSet<>();
        int navigationSignals = 0;

        for (String endpoint : safeList(entryPoint.endpoints())) {
            EndpointPath endpointPath = parseEndpointPath(endpoint);
            routeFamilies.add(endpointPath.family());
            routeSignatures.add(endpointPath.signature());
            if (endpointPath.navigationHeavy()) {
                navigationSignals++;
            }
        }

        boolean navigationHeavy = navigationSignals >= NAVIGATION_ROUTE_THRESHOLD
                || (navigationSignals >= 1 && safeList(entryPoint.endpoints()).size() >= 4);
        return new RouteProfile(routeFamilies.size(), routeSignatures.size(), navigationHeavy);
    }

    private boolean isMultiPurpose(
            boolean broadEndpointSurface,
            boolean broadDependencySurface,
            boolean mixedRoutingPattern,
            boolean navigationHeavy,
            boolean heterogeneousDownstreamTargets
    ) {
        if (navigationHeavy && broadEndpointSurface) {
            return true;
        }
        if (heterogeneousDownstreamTargets && (broadDependencySurface || broadEndpointSurface || navigationHeavy)) {
            return true;
        }
        if (broadEndpointSurface && broadDependencySurface && mixedRoutingPattern) {
            return true;
        }
        return false;
    }

    private DownstreamProfile buildDownstreamProfile(List<RepresentativeFlow> flows) {
        Set<String> secondHopFamilies = new LinkedHashSet<>();
        Set<String> terminalFamilies = new LinkedHashSet<>();

        for (RepresentativeFlow flow : safeFlowList(flows)) {
            List<String> classNames = safeList(flow.classNames());
            if (classNames.size() >= 2) {
                secondHopFamilies.add(normalizeDomainToken(classNames.get(1)));
            }
            if (!classNames.isEmpty()) {
                terminalFamilies.add(normalizeDomainToken(classNames.get(classNames.size() - 1)));
            }
        }

        boolean heterogeneousTargets = countMeaningfulFamilies(secondHopFamilies) >= 2
                || countMeaningfulFamilies(terminalFamilies) >= 2;
        return new DownstreamProfile(heterogeneousTargets);
    }

    private int countMeaningfulFamilies(Set<String> families) {
        int count = 0;
        for (String family : families) {
            if (!family.isBlank() && !"Unknown".equals(family)) {
                count++;
            }
        }
        return count;
    }

    private EndpointPath parseEndpointPath(String endpoint) {
        String normalizedEndpoint = endpoint == null ? "" : endpoint.trim();
        int arrowIndex = normalizedEndpoint.indexOf("->");
        String routePart = arrowIndex >= 0
                ? normalizedEndpoint.substring(0, arrowIndex).trim()
                : normalizedEndpoint;
        String[] routeTokens = routePart.split("\\s+");
        String rawPath = routeTokens.length >= 2 ? routeTokens[1].trim() : "";

        List<String> segments = new ArrayList<>();
        for (String rawSegment : rawPath.split("/")) {
            String segment = rawSegment.trim();
            if (segment.isEmpty() || (segment.startsWith("{") && segment.endsWith("}"))) {
                continue;
            }
            segments.add(segment);
        }

        String family = segments.isEmpty() ? "root" : segments.get(0);
        String signature = segments.isEmpty() ? "root" : String.join("/", segments);
        boolean navigationHeavy = "root".equals(family) || NAVIGATION_ROUTE_TOKENS.contains(family);
        return new EndpointPath(family, signature, navigationHeavy);
    }

    private boolean isEntryPointLike(ContextSnapshot snapshot) {
        return "EntryPointLike".equals(normalize(snapshot.architectureAffinity()))
                || "Controller".equals(normalize(snapshot.classRole()))
                || !safeList(snapshot.endpoints()).isEmpty();
    }

    private int commonPackageDepth(String sourcePackage, String candidatePackage) {
        List<String> sourceParts = splitPackage(sourcePackage);
        List<String> candidateParts = splitPackage(candidatePackage);
        int max = Math.min(sourceParts.size(), candidateParts.size());
        int common = 0;

        while (common < max && sourceParts.get(common).equals(candidateParts.get(common))) {
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

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<ContextSnapshot> safeContextList(List<ContextSnapshot> values) {
        return values == null ? List.of() : values;
    }

    private List<RepresentativeFlow> safeFlowList(List<RepresentativeFlow> values) {
        return values == null ? List.of() : values;
    }

    private String snapshotIdentity(ContextSnapshot snapshot) {
        if (snapshot == null) {
            return "Unknown";
        }
        return normalize(snapshot.packageName())
                + "#"
                + normalize(snapshot.className())
                + "#"
                + normalize(snapshot.filePath());
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

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }

    private record RouteProfile(int distinctFamilies, int distinctSignatures, boolean navigationHeavy) {
    }

    private record DownstreamProfile(boolean heterogeneousTargets) {
    }

    private record EndpointPath(String family, String signature, boolean navigationHeavy) {
    }
}
