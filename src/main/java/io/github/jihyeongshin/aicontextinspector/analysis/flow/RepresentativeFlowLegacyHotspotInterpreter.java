package io.github.jihyeongshin.aicontextinspector.analysis.flow;

import io.github.jihyeongshin.aicontextinspector.model.source.ContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.flow.LegacyHotspotLevel;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowLegacyHotspotInterpretation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RepresentativeFlowLegacyHotspotInterpreter {
    private static final int SERVICE_HUB_DEPENDENCY_THRESHOLD = 8;
    private static final String NOTE_WEAK_SERVICE_TO_SERVICE_HOP = "weak service-to-service hop";
    private static final String NOTE_ORCHESTRATION_HEAVY_SERVICE_HUB = "orchestration-heavy service hub";
    private static final String NOTE_TRANSITIONAL_SERVICE_LAYER = "transitional service layer";
    private static final String NOTE_MIXED_DOWNSTREAM_COORDINATION = "mixed downstream coordination";
    private static final String NOTE_LEGACY_HEAVY_ACCEPTABLE_FLOW = "legacy-heavy acceptable flow";
    private static final Set<String> COORDINATION_ROLES = Set.of(
            "Service",
            "Repository",
            "Mapper",
            "Client",
            "Adapter"
    );

    public Map<String, RepresentativeFlowLegacyHotspotInterpretation> evaluate(
            ProjectContextSnapshot snapshot,
            List<RepresentativeFlow> flows
    ) {
        if (snapshot == null || snapshot.isEmpty() || flows == null || flows.isEmpty()) {
            return Map.of();
        }

        Map<String, List<ContextSnapshot>> classesByName = indexByClassName(snapshot.files());
        Map<String, RepresentativeFlowLegacyHotspotInterpretation> interpretations = new LinkedHashMap<>();

        for (RepresentativeFlow flow : safeFlowList(flows)) {
            interpretations.put(flow.toDisplayString(), interpretFlow(flow, classesByName));
        }

        return Map.copyOf(interpretations);
    }

    private RepresentativeFlowLegacyHotspotInterpretation interpretFlow(
            RepresentativeFlow flow,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        List<String> classNames = safeList(flow.classNames());
        List<String> classRoles = safeList(flow.classRoles());
        List<ContextSnapshot> resolvedNodes = resolveFlowNodes(classNames, classRoles, classesByName);

        boolean serviceToServiceHop = hasServiceToServiceHop(classRoles);
        boolean repeatedServiceToServiceChain = countServiceToServiceTransitions(classRoles) >= 2;
        boolean orchestrationHeavyServiceHub =
                hasOrchestrationHeavyServiceHub(resolvedNodes, classRoles, classesByName);
        boolean mixedDownstreamCoordination =
                hasMixedDownstreamCoordination(resolvedNodes, classRoles, classesByName);
        boolean transitionalServiceLayer =
                hasTransitionalServiceLayer(classRoles, serviceToServiceHop, mixedDownstreamCoordination);
        boolean legacyHeavyAcceptableFlow =
                (serviceToServiceHop || transitionalServiceLayer) && classNames.size() >= 3;

        int hotspotScore = 0;
        if (serviceToServiceHop) {
            hotspotScore += 2;
        }
        if (repeatedServiceToServiceChain) {
            hotspotScore += 1;
        }
        if (transitionalServiceLayer) {
            hotspotScore += 1;
        }
        if (orchestrationHeavyServiceHub) {
            hotspotScore += 1;
        }
        if (mixedDownstreamCoordination) {
            hotspotScore += 1;
        }

        List<String> notes = new ArrayList<>();
        if (serviceToServiceHop) {
            notes.add(NOTE_WEAK_SERVICE_TO_SERVICE_HOP);
        }
        if (orchestrationHeavyServiceHub) {
            notes.add(NOTE_ORCHESTRATION_HEAVY_SERVICE_HUB);
        }
        if (transitionalServiceLayer) {
            notes.add(NOTE_TRANSITIONAL_SERVICE_LAYER);
        }
        if (mixedDownstreamCoordination) {
            notes.add(NOTE_MIXED_DOWNSTREAM_COORDINATION);
        }
        if (legacyHeavyAcceptableFlow) {
            notes.add(NOTE_LEGACY_HEAVY_ACCEPTABLE_FLOW);
        }

        return new RepresentativeFlowLegacyHotspotInterpretation(classifyHotspot(hotspotScore), notes);
    }

    private LegacyHotspotLevel classifyHotspot(int hotspotScore) {
        if (hotspotScore >= 4) {
            return LegacyHotspotLevel.HIGH;
        }
        if (hotspotScore >= 2) {
            return LegacyHotspotLevel.POSSIBLE;
        }
        return LegacyHotspotLevel.NONE;
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

    private boolean hasServiceToServiceHop(List<String> classRoles) {
        return countServiceToServiceTransitions(classRoles) >= 1;
    }

    private int countServiceToServiceTransitions(List<String> classRoles) {
        int count = 0;
        for (int index = 0; index < classRoles.size() - 1; index++) {
            if ("Service".equals(normalize(classRoles.get(index)))
                    && "Service".equals(normalize(classRoles.get(index + 1)))) {
                count++;
            }
        }
        return count;
    }

    private boolean hasOrchestrationHeavyServiceHub(
            List<ContextSnapshot> resolvedNodes,
            List<String> classRoles,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        for (int index = 0; index < resolvedNodes.size(); index++) {
            ContextSnapshot node = resolvedNodes.get(index);
            String role = index < classRoles.size() ? normalize(classRoles.get(index)) : normalize(node.classRole());
            if (!"Service".equals(role)) {
                continue;
            }

            if (safeList(node.dependencies()).size() >= SERVICE_HUB_DEPENDENCY_THRESHOLD
                    && countCoordinationRoleFamilies(node, classesByName) >= 2) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMixedDownstreamCoordination(
            List<ContextSnapshot> resolvedNodes,
            List<String> classRoles,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        for (int index = 0; index < resolvedNodes.size(); index++) {
            ContextSnapshot node = resolvedNodes.get(index);
            String role = index < classRoles.size() ? normalize(classRoles.get(index)) : normalize(node.classRole());
            if (!"Service".equals(role)) {
                continue;
            }

            if (countCoordinationRoleFamilies(node, classesByName) >= 3) {
                return true;
            }
        }
        return false;
    }

    private int countCoordinationRoleFamilies(
            ContextSnapshot node,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        Set<String> roleFamilies = new LinkedHashSet<>();

        for (String dependencyName : safeList(node.dependencies())) {
            for (ContextSnapshot candidate : safeContextList(classesByName.getOrDefault(dependencyName, List.of()))) {
                String role = normalize(candidate.classRole());
                if (COORDINATION_ROLES.contains(role)) {
                    roleFamilies.add(role);
                }
            }
        }

        return roleFamilies.size();
    }

    private boolean hasTransitionalServiceLayer(
            List<String> classRoles,
            boolean serviceToServiceHop,
            boolean mixedDownstreamCoordination
    ) {
        if (classRoles.size() < 2) {
            return false;
        }
        return "Controller".equals(normalize(classRoles.get(0)))
                && "Service".equals(normalize(classRoles.get(1)))
                && (serviceToServiceHop || mixedDownstreamCoordination);
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

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<ContextSnapshot> safeContextList(List<ContextSnapshot> values) {
        return values == null ? List.of() : values;
    }

    private List<RepresentativeFlow> safeFlowList(List<RepresentativeFlow> values) {
        return values == null ? List.of() : values;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }
}

