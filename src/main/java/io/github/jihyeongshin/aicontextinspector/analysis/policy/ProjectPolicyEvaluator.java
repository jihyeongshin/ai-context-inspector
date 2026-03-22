package io.github.jihyeongshin.aicontextinspector.analysis.policy;

import io.github.jihyeongshin.aicontextinspector.analysis.flow.RepresentativeFlowEntryPointInterpreter;
import io.github.jihyeongshin.aicontextinspector.analysis.flow.RepresentativeFlowLegacyHotspotInterpreter;
import io.github.jihyeongshin.aicontextinspector.analysis.flow.RepresentativeFlowMetadataEvaluator;
import io.github.jihyeongshin.aicontextinspector.analysis.rule.ProjectRuleEvaluator;

import io.github.jihyeongshin.aicontextinspector.model.flow.EntryPointInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.FlowAmbiguity;
import io.github.jihyeongshin.aicontextinspector.model.flow.FlowConfidence;
import io.github.jihyeongshin.aicontextinspector.model.flow.InterpretedRepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.flow.LegacyHotspotLevel;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.policy.ProjectPolicyCaution;
import io.github.jihyeongshin.aicontextinspector.model.policy.ProjectPolicyEvidence;
import io.github.jihyeongshin.aicontextinspector.model.policy.ProjectPolicySignal;
import io.github.jihyeongshin.aicontextinspector.model.policy.ProjectPolicySnapshot;
import io.github.jihyeongshin.aicontextinspector.model.policy.ProjectPolicyStatus;
import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRule;
import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRuleSignal;
import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRuleSignalStatus;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowEntryPointInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowLegacyHotspotInterpretation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ProjectPolicyEvaluator {
    private final RepresentativeFlowMetadataEvaluator representativeFlowMetadataEvaluator =
            new RepresentativeFlowMetadataEvaluator();
    private final RepresentativeFlowEntryPointInterpreter representativeFlowEntryPointInterpreter =
            new RepresentativeFlowEntryPointInterpreter();
    private final RepresentativeFlowLegacyHotspotInterpreter representativeFlowLegacyHotspotInterpreter =
            new RepresentativeFlowLegacyHotspotInterpreter();
    private final ProjectRuleEvaluator projectRuleEvaluator = new ProjectRuleEvaluator();

    public ProjectPolicySnapshot evaluate(ProjectContextSnapshot snapshot, List<RepresentativeFlow> flows) {
        if (snapshot == null || flows == null || flows.isEmpty()) {
            return new ProjectPolicySnapshot(
                    ProjectPolicyStatus.NOT_APPLICABLE,
                    new ProjectPolicyEvidence(0, snapshot == null ? 0 : snapshot.rulesLoadedCount(), 0, 0, 0, 0, 0),
                    List.of(new ProjectPolicySignal(
                            "policy-evidence",
                            ProjectPolicyStatus.NOT_APPLICABLE,
                            "Current extracted evidence does not include representative flows for policy interpretation."
                    )),
                    snapshot != null && !snapshot.hasProjectRules()
                            ? List.of(ProjectPolicyCaution.RULE_INPUT_MISSING)
                            : List.of()
            );
        }

        List<InterpretedRepresentativeFlow> interpretedFlows =
                representativeFlowMetadataEvaluator.evaluate(snapshot, flows);
        Map<String, RepresentativeFlowEntryPointInterpretation> entryPointInterpretations =
                representativeFlowEntryPointInterpreter.evaluate(snapshot, flows);
        Map<String, RepresentativeFlowLegacyHotspotInterpretation> hotspotInterpretations =
                representativeFlowLegacyHotspotInterpreter.evaluate(snapshot, flows);
        List<ProjectRuleSignal> ruleSignals = projectRuleEvaluator.evaluate(snapshot, flows);

        int ambiguousFlows = (int) interpretedFlows.stream()
                .filter(flow -> flow.metadata().ambiguity() != FlowAmbiguity.NONE)
                .count();
        int lowConfidenceFlows = (int) interpretedFlows.stream()
                .filter(flow -> flow.metadata().confidence() == FlowConfidence.LOW)
                .count();
        int hotspotFlows = (int) hotspotInterpretations.values().stream()
                .filter(interpretation -> interpretation.legacyHotspot() == LegacyHotspotLevel.POSSIBLE
                        || interpretation.legacyHotspot() == LegacyHotspotLevel.HIGH)
                .count();
        int unknownAffinityCount = (int) snapshot.files().stream()
                .filter(file -> "Unknown".equals(normalize(file.architectureAffinity())))
                .count();
        int multiPurposeEntryPoints = (int) entryPointInterpretations.values().stream()
                .filter(interpretation -> interpretation.interpretation() == EntryPointInterpretation.MULTI_PURPOSE)
                .count();

        ProjectPolicyEvidence evidence = new ProjectPolicyEvidence(
                flows.size(),
                snapshot.rulesLoadedCount(),
                ambiguousFlows,
                hotspotFlows,
                lowConfidenceFlows,
                unknownAffinityCount,
                multiPurposeEntryPoints
        );
        List<ProjectPolicyCaution> cautions = buildCautions(snapshot, evidence);
        List<ProjectPolicySignal> signals = buildSignals(snapshot, ruleSignals, evidence);
        ProjectPolicyStatus overallStatus = determineOverallStatus(snapshot, signals);

        return new ProjectPolicySnapshot(overallStatus, evidence, signals, cautions);
    }

    public Map<String, Long> countSignalsByStatus(ProjectPolicySnapshot policySnapshot) {
        if (policySnapshot == null) {
            return Map.of();
        }

        return policySnapshot.signals().stream()
                .collect(Collectors.groupingBy(
                        signal -> signal.status().displayName(),
                        TreeMap::new,
                        Collectors.counting()
                ));
    }

    private List<ProjectPolicyCaution> buildCautions(
            ProjectContextSnapshot snapshot,
            ProjectPolicyEvidence evidence
    ) {
        EnumSet<ProjectPolicyCaution> cautions = EnumSet.noneOf(ProjectPolicyCaution.class);
        if (!snapshot.hasProjectRules()) {
            cautions.add(ProjectPolicyCaution.RULE_INPUT_MISSING);
        }
        if (evidence.ambiguousFlowCount() > 0) {
            cautions.add(ProjectPolicyCaution.AMBIGUITY_PRESENT);
        }
        if (evidence.hotspotFlowCount() > 0) {
            cautions.add(ProjectPolicyCaution.HOTSPOT_PRESENT);
        }
        if (evidence.lowConfidenceFlowCount() > 0) {
            cautions.add(ProjectPolicyCaution.LOW_CONFIDENCE_PRESENT);
        }
        if (evidence.unknownAffinityCount() > 0) {
            cautions.add(ProjectPolicyCaution.UNKNOWN_AFFINITY_PRESENT);
        }
        return List.copyOf(cautions);
    }

    private List<ProjectPolicySignal> buildSignals(
            ProjectContextSnapshot snapshot,
            List<ProjectRuleSignal> ruleSignals,
            ProjectPolicyEvidence evidence
    ) {
        if (snapshot.hasProjectRules() && !ruleSignals.isEmpty()) {
            Map<String, ProjectRule> rulesById = snapshot.projectRuleSet().rules().stream()
                    .collect(Collectors.toMap(
                            ProjectRule::id,
                            rule -> rule,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));
            List<ProjectPolicySignal> signals = new ArrayList<>();
            for (ProjectRuleSignal ruleSignal : ruleSignals) {
                signals.add(new ProjectPolicySignal(
                        ruleSignal.ruleId(),
                        mapStatus(ruleSignal.status()),
                        buildRulePolicySummary(rulesById.get(ruleSignal.ruleId()), ruleSignal)
                ));
            }
            return List.copyOf(signals);
        }

        return List.of(new ProjectPolicySignal(
                "policy-evidence",
                ProjectPolicyStatus.WEAK_SIGNAL,
                buildStructureOnlySummary(evidence)
        ));
    }

    private ProjectPolicyStatus mapStatus(ProjectRuleSignalStatus ruleStatus) {
        if (ruleStatus == null) {
            return ProjectPolicyStatus.WEAK_SIGNAL;
        }
        return switch (ruleStatus) {
            case ALIGNED -> ProjectPolicyStatus.ALIGNED;
            case POSSIBLE_DRIFT -> ProjectPolicyStatus.MIXED;
            case NOT_APPLICABLE -> ProjectPolicyStatus.NOT_APPLICABLE;
            case LOAD_WARNING -> ProjectPolicyStatus.WEAK_SIGNAL;
        };
    }

    private ProjectPolicyStatus determineOverallStatus(
            ProjectContextSnapshot snapshot,
            List<ProjectPolicySignal> signals
    ) {
        if (signals.isEmpty()) {
            return snapshot.hasProjectRules() ? ProjectPolicyStatus.NOT_APPLICABLE : ProjectPolicyStatus.WEAK_SIGNAL;
        }
        if (signals.stream().anyMatch(signal -> signal.status() == ProjectPolicyStatus.MIXED)) {
            return ProjectPolicyStatus.MIXED;
        }
        if (signals.stream().allMatch(signal -> signal.status() == ProjectPolicyStatus.NOT_APPLICABLE)) {
            return ProjectPolicyStatus.NOT_APPLICABLE;
        }
        if (!snapshot.hasProjectRules()) {
            return ProjectPolicyStatus.WEAK_SIGNAL;
        }
        return ProjectPolicyStatus.ALIGNED;
    }

    private String buildRulePolicySummary(ProjectRule rule, ProjectRuleSignal signal) {
        if (rule == null) {
            return signal.summary();
        }

        String affinityList = !rule.affinityAnyOf().isEmpty()
                ? formatAffinityList(rule.affinityAnyOf())
                : formatAffinityList(rule.toAffinityAnyOf());
        return switch (rule.kind()) {
            case EXPECTED_TRANSITION -> buildTransitionSummary(rule, signal);
            case PREFERRED_TERMINAL_AFFINITY -> buildTerminalSummary(affinityList, signal);
            case DISCOURAGED_MID_AFFINITY -> buildMidFlowSummary(affinityList, signal);
        };
    }

    private String buildTransitionSummary(ProjectRule rule, ProjectRuleSignal signal) {
        if (signal.status() == ProjectRuleSignalStatus.ALIGNED) {
            return "Representative flows generally hand off " + rule.fromAffinity()
                    + " responsibilities to " + formatAffinityList(rule.toAffinityAnyOf())
                    + " responsibilities with strong current evidence.";
        }
        if (signal.status() == ProjectRuleSignalStatus.POSSIBLE_DRIFT) {
            return "Representative flows generally hand off " + rule.fromAffinity()
                    + " responsibilities to " + formatAffinityList(rule.toAffinityAnyOf())
                    + " responsibilities, but " + signal.summary() + ".";
        }
        return "Current extracted evidence does not make this transition rule applicable to representative flows.";
    }

    private String buildTerminalSummary(String affinityList, ProjectRuleSignal signal) {
        if (signal.status() == ProjectRuleSignalStatus.ALIGNED) {
            return "Representative terminal responsibilities are mostly " + affinityList
                    + " aligned.";
        }
        if (signal.status() == ProjectRuleSignalStatus.POSSIBLE_DRIFT) {
            return "Representative terminal responsibilities are mostly " + affinityList
                    + " aligned, but " + signal.summary() + ".";
        }
        return "Current extracted evidence does not make this terminal affinity rule applicable to representative flows.";
    }

    private String buildMidFlowSummary(String affinityList, ProjectRuleSignal signal) {
        boolean entryPointOnly = "EntryPointLike".equals(affinityList);
        if (signal.status() == ProjectRuleSignalStatus.ALIGNED) {
            return entryPointOnly
                    ? "EntryPointLike responsibilities do not reappear in representative flow middles."
                    : affinityList + " dominance is not observed in representative flow middles.";
        }
        if (signal.status() == ProjectRuleSignalStatus.POSSIBLE_DRIFT) {
            return entryPointOnly
                    ? "EntryPointLike responsibilities rarely reappear in representative flow middles, but "
                    + signal.summary() + "."
                    : affinityList + " dominance is not common in representative flow middles, but "
                    + signal.summary() + ".";
        }
        return "Current extracted evidence does not make this mid-flow affinity rule applicable to representative flows.";
    }

    private String buildStructureOnlySummary(ProjectPolicyEvidence evidence) {
        return "Current extracted evidence suggests representative flows remain broadly layered, but project rule input is not available."
                + " Evidence is based on "
                + formatCount(evidence.representativeFlowCount(), "representative flow")
                + ".";
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }

    private String formatAffinityList(List<String> affinities) {
        if (affinities == null || affinities.isEmpty()) {
            return "Unknown";
        }
        if (affinities.size() == 1) {
            return affinities.get(0);
        }
        if (affinities.size() == 2) {
            return affinities.get(0) + " or " + affinities.get(1);
        }
        return String.join(", ", affinities.subList(0, affinities.size() - 1))
                + ", or "
                + affinities.get(affinities.size() - 1);
    }

    private String formatCount(int count, String singularNoun) {
        return count + " " + (count == 1 ? singularNoun : singularNoun + "s");
    }
}

