package io.github.jihyeongshin.aicontextinspector.project;

import io.github.jihyeongshin.aicontextinspector.model.ContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.ProjectRule;
import io.github.jihyeongshin.aicontextinspector.model.ProjectRuleKind;
import io.github.jihyeongshin.aicontextinspector.model.ProjectRuleSet;
import io.github.jihyeongshin.aicontextinspector.model.ProjectRuleSignal;
import io.github.jihyeongshin.aicontextinspector.model.ProjectRuleSignalStatus;
import io.github.jihyeongshin.aicontextinspector.model.RepresentativeFlow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProjectRuleEvaluator {
    public List<ProjectRuleSignal> evaluate(ProjectContextSnapshot snapshot, List<RepresentativeFlow> flows) {
        if (snapshot == null || snapshot.projectRuleSet() == null) {
            return List.of();
        }

        ProjectRuleSet ruleSet = snapshot.projectRuleSet();
        if (ruleSet.rules().isEmpty()) {
            return List.of();
        }

        Map<String, List<ContextSnapshot>> classesByName = indexByClassName(snapshot.files());
        List<ProjectRuleSignal> signals = new ArrayList<>();
        for (ProjectRule rule : ruleSet.rules()) {
            signals.add(evaluateRule(rule, flows, classesByName));
        }
        return List.copyOf(signals);
    }

    private ProjectRuleSignal evaluateRule(
            ProjectRule rule,
            List<RepresentativeFlow> flows,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        int aligned = 0;
        int drift = 0;
        int applicable = 0;

        for (RepresentativeFlow flow : safeFlowList(flows)) {
            RuleMatchResult result = evaluateRuleAgainstFlow(rule, flow, classesByName);
            if (!result.applicable()) {
                continue;
            }

            applicable++;
            if (result.aligned()) {
                aligned++;
            } else {
                drift++;
            }
        }

        if (applicable == 0) {
            return new ProjectRuleSignal(
                    rule.id(),
                    ProjectRuleSignalStatus.NOT_APPLICABLE,
                    0,
                    0,
                    0,
                    "not applicable to current representative flows"
            );
        }

        if (drift > 0) {
            return new ProjectRuleSignal(
                    rule.id(),
                    ProjectRuleSignalStatus.POSSIBLE_DRIFT,
                    aligned,
                    drift,
                    applicable,
                    "possible drift in " + drift + " representative flows"
            );
        }

        return new ProjectRuleSignal(
                rule.id(),
                ProjectRuleSignalStatus.ALIGNED,
                aligned,
                0,
                applicable,
                "aligned with " + aligned + " representative flows"
        );
    }

    private RuleMatchResult evaluateRuleAgainstFlow(
            ProjectRule rule,
            RepresentativeFlow flow,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        List<ContextSnapshot> resolvedNodes = resolveFlowNodes(flow, classesByName);
        if (resolvedNodes.isEmpty()) {
            return RuleMatchResult.notApplicable();
        }

        return switch (rule.kind()) {
            case EXPECTED_TRANSITION -> evaluateExpectedTransition(rule, resolvedNodes);
            case PREFERRED_TERMINAL_AFFINITY -> evaluatePreferredTerminalAffinity(rule, resolvedNodes);
            case DISCOURAGED_MID_AFFINITY -> evaluateDiscouragedMidAffinity(rule, resolvedNodes);
        };
    }

    private RuleMatchResult evaluateExpectedTransition(ProjectRule rule, List<ContextSnapshot> resolvedNodes) {
        if (resolvedNodes.size() < 2) {
            return RuleMatchResult.notApplicable();
        }

        String fromAffinity = normalize(resolvedNodes.get(0).architectureAffinity());
        if (!fromAffinity.equals(rule.fromAffinity())) {
            return RuleMatchResult.notApplicable();
        }

        String toAffinity = normalize(resolvedNodes.get(1).architectureAffinity());
        return RuleMatchResult.applicable(rule.toAffinityAnyOf().contains(toAffinity));
    }

    private RuleMatchResult evaluatePreferredTerminalAffinity(ProjectRule rule, List<ContextSnapshot> resolvedNodes) {
        if (resolvedNodes.isEmpty()) {
            return RuleMatchResult.notApplicable();
        }

        String terminalAffinity = normalize(resolvedNodes.get(resolvedNodes.size() - 1).architectureAffinity());
        return RuleMatchResult.applicable(rule.affinityAnyOf().contains(terminalAffinity));
    }

    private RuleMatchResult evaluateDiscouragedMidAffinity(ProjectRule rule, List<ContextSnapshot> resolvedNodes) {
        if (resolvedNodes.size() < 3) {
            return RuleMatchResult.notApplicable();
        }

        for (int index = 1; index < resolvedNodes.size() - 1; index++) {
            String midAffinity = normalize(resolvedNodes.get(index).architectureAffinity());
            if (rule.affinityAnyOf().contains(midAffinity)) {
                return RuleMatchResult.applicable(false);
            }
        }
        return RuleMatchResult.applicable(true);
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
            RepresentativeFlow flow,
            Map<String, List<ContextSnapshot>> classesByName
    ) {
        List<ContextSnapshot> resolvedNodes = new ArrayList<>();
        ContextSnapshot previous = null;
        List<String> classNames = safeList(flow.classNames());
        List<String> classRoles = safeList(flow.classRoles());

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

        return candidates.stream()
                .max(Comparator
                        .comparingInt((ContextSnapshot candidate) -> roleMatchScore(candidate, classRole))
                        .thenComparingInt(candidate -> previous == null
                                ? entryPointResolutionScore(candidate)
                                : commonPackageDepth(previous.packageName(), candidate.packageName()))
                        .thenComparing(candidate -> normalize(candidate.filePath()), Comparator.reverseOrder()))
                .orElse(null);
    }

    private int roleMatchScore(ContextSnapshot candidate, String classRole) {
        return normalize(candidate.classRole()).equals(classRole) ? 2 : 0;
    }

    private int entryPointResolutionScore(ContextSnapshot candidate) {
        int score = 0;
        if ("EntryPointLike".equals(normalize(candidate.architectureAffinity()))) {
            score += 2;
        }
        if ("Controller".equals(normalize(candidate.classRole()))) {
            score += 1;
        }
        return score;
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

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }

    private record RuleMatchResult(boolean applicable, boolean aligned) {
        static RuleMatchResult applicable(boolean aligned) {
            return new RuleMatchResult(true, aligned);
        }

        static RuleMatchResult notApplicable() {
            return new RuleMatchResult(false, false);
        }
    }
}
