package io.github.jihyeongshin.aicontextinspector.analysis.guidance;

import io.github.jihyeongshin.aicontextinspector.analysis.flow.RepresentativeFlowEntryPointInterpreter;
import io.github.jihyeongshin.aicontextinspector.analysis.flow.RepresentativeFlowLegacyHotspotInterpreter;
import io.github.jihyeongshin.aicontextinspector.analysis.flow.RepresentativeFlowMetadataEvaluator;
import io.github.jihyeongshin.aicontextinspector.analysis.policy.ProjectPolicyEvaluator;
import io.github.jihyeongshin.aicontextinspector.analysis.rule.ProjectRuleEvaluator;
import io.github.jihyeongshin.aicontextinspector.model.flow.EntryPointInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.FlowAmbiguity;
import io.github.jihyeongshin.aicontextinspector.model.flow.FlowConfidence;
import io.github.jihyeongshin.aicontextinspector.model.flow.InterpretedRepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.flow.LegacyHotspotLevel;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowEntryPointInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowLegacyHotspotInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.guidance.GuidanceSignal;
import io.github.jihyeongshin.aicontextinspector.model.guidance.GuidanceSignalStatus;
import io.github.jihyeongshin.aicontextinspector.model.guidance.ReadingGuidanceSummary;
import io.github.jihyeongshin.aicontextinspector.model.policy.ProjectPolicySnapshot;
import io.github.jihyeongshin.aicontextinspector.model.policy.ProjectPolicyStatus;
import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRuleSignal;
import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRuleSignalStatus;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ReadingGuidanceEvaluator {
    private static final int ORIENTATION_SAMPLE_LIMIT = 3;

    private final RepresentativeFlowMetadataEvaluator representativeFlowMetadataEvaluator =
            new RepresentativeFlowMetadataEvaluator();
    private final RepresentativeFlowEntryPointInterpreter representativeFlowEntryPointInterpreter =
            new RepresentativeFlowEntryPointInterpreter();
    private final RepresentativeFlowLegacyHotspotInterpreter representativeFlowLegacyHotspotInterpreter =
            new RepresentativeFlowLegacyHotspotInterpreter();
    private final ProjectRuleEvaluator projectRuleEvaluator = new ProjectRuleEvaluator();
    private final ProjectPolicyEvaluator projectPolicyEvaluator = new ProjectPolicyEvaluator();

    public ReadingGuidanceSummary evaluate(ProjectContextSnapshot snapshot, List<RepresentativeFlow> flows) {
        if (snapshot == null || flows == null || flows.isEmpty()) {
            return new ReadingGuidanceSummary(
                    List.of("Current extracted evidence does not yet provide representative flows for compact reading guidance."),
                    List.of(new GuidanceSignal(
                            "Orientation",
                            GuidanceSignalStatus.NOT_APPLICABLE,
                            "Current extracted evidence does not yet support a representative reading path."
                    )),
                    List.of("Guidance remains limited to current extracted evidence and should not be read as codebase truth.")
            );
        }

        List<InterpretedRepresentativeFlow> interpretedFlows =
                representativeFlowMetadataEvaluator.evaluate(snapshot, flows);
        Map<String, RepresentativeFlowEntryPointInterpretation> entryPointInterpretations =
                representativeFlowEntryPointInterpreter.evaluate(snapshot, flows);
        Map<String, RepresentativeFlowLegacyHotspotInterpretation> hotspotInterpretations =
                representativeFlowLegacyHotspotInterpreter.evaluate(snapshot, flows);
        List<ProjectRuleSignal> ruleSignals = projectRuleEvaluator.evaluate(snapshot, flows);
        ProjectPolicySnapshot policySnapshot = projectPolicyEvaluator.evaluate(snapshot, flows);

        GuidanceEvidence evidence = buildEvidence(
                snapshot,
                flows,
                interpretedFlows,
                entryPointInterpretations,
                hotspotInterpretations,
                ruleSignals,
                policySnapshot
        );

        List<GuidanceSignal> signals = List.of(
                buildOrientationSignal(evidence),
                buildTrustSignal(evidence),
                buildCautionSignal(evidence),
                buildRulePolicySignal(evidence),
                buildUnknownSignal(evidence)
        );

        List<String> summaryLines = buildSummaryLines(evidence);
        List<String> notes = buildNotes(evidence);
        return new ReadingGuidanceSummary(summaryLines, signals, notes);
    }

    private GuidanceEvidence buildEvidence(
            ProjectContextSnapshot snapshot,
            List<RepresentativeFlow> flows,
            List<InterpretedRepresentativeFlow> interpretedFlows,
            Map<String, RepresentativeFlowEntryPointInterpretation> entryPointInterpretations,
            Map<String, RepresentativeFlowLegacyHotspotInterpretation> hotspotInterpretations,
            List<ProjectRuleSignal> ruleSignals,
            ProjectPolicySnapshot policySnapshot
    ) {
        Map<String, Long> rolePatternCounts = flows.stream()
                .collect(Collectors.groupingBy(
                        RepresentativeFlow::toRoleDisplayString,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        String dominantRolePattern = rolePatternCounts.isEmpty()
                ? "Unknown"
                : sortByCountDescending(rolePatternCounts).get(0).getKey();
        List<String> orientationStarts = flows.stream()
                .map(this::startClassName)
                .filter(name -> !"Unknown".equals(name))
                .distinct()
                .limit(ORIENTATION_SAMPLE_LIMIT)
                .toList();

        int ambiguousFlowCount = (int) interpretedFlows.stream()
                .filter(flow -> flow.metadata().ambiguity() != FlowAmbiguity.NONE)
                .count();
        int lowConfidenceFlowCount = (int) interpretedFlows.stream()
                .filter(flow -> flow.metadata().confidence() == FlowConfidence.LOW)
                .count();
        int hotspotFlowCount = (int) hotspotInterpretations.values().stream()
                .filter(interpretation -> interpretation.legacyHotspot() == LegacyHotspotLevel.POSSIBLE
                        || interpretation.legacyHotspot() == LegacyHotspotLevel.HIGH)
                .count();
        int multiPurposeEntryPointCount = (int) entryPointInterpretations.values().stream()
                .filter(interpretation -> interpretation.interpretation() == EntryPointInterpretation.MULTI_PURPOSE)
                .count();
        int unknownAffinityCount = (int) snapshot.files().stream()
                .filter(file -> "Unknown".equals(normalize(file.architectureAffinity())))
                .count();
        int alignedRuleSignals = (int) ruleSignals.stream()
                .filter(signal -> signal.status() == ProjectRuleSignalStatus.ALIGNED)
                .count();
        int driftRuleSignals = (int) ruleSignals.stream()
                .filter(signal -> signal.status() == ProjectRuleSignalStatus.POSSIBLE_DRIFT)
                .count();
        int notApplicableRuleSignals = (int) ruleSignals.stream()
                .filter(signal -> signal.status() == ProjectRuleSignalStatus.NOT_APPLICABLE)
                .count();

        return new GuidanceEvidence(
                snapshot.files().size(),
                flows.size(),
                dominantRolePattern,
                orientationStarts,
                ambiguousFlowCount,
                lowConfidenceFlowCount,
                hotspotFlowCount,
                multiPurposeEntryPointCount,
                unknownAffinityCount,
                snapshot.rulesLoadedCount(),
                alignedRuleSignals,
                driftRuleSignals,
                notApplicableRuleSignals,
                policySnapshot
        );
    }

    private GuidanceSignal buildOrientationSignal(GuidanceEvidence evidence) {
        GuidanceSignalStatus status = determineOrientationStatus(evidence);
        String orientationSamples = evidence.orientationStarts().isEmpty()
                ? "the top representative entry points"
                : joinWithAnd(evidence.orientationStarts());

        String message;
        if (status == GuidanceSignalStatus.STRONG_ORIENTATION) {
            message = "Representative flows suggest the current project is easiest to read through "
                    + evidence.dominantRolePattern()
                    + " shaped paths, starting with "
                    + orientationSamples
                    + ".";
        } else if (status == GuidanceSignalStatus.PROVISIONAL) {
            message = "Representative flows suggest starting with "
                    + evidence.dominantRolePattern()
                    + " shaped paths such as "
                    + orientationSamples
                    + ", but this orientation should stay provisional.";
        } else {
            message = "Current extracted evidence does not yet support a stable orientation path.";
        }

        return new GuidanceSignal("Where to read first", status, message);
    }

    private GuidanceSignal buildTrustSignal(GuidanceEvidence evidence) {
        GuidanceSignalStatus status = determineTrustStatus(evidence);
        String message;
        if (status == GuidanceSignalStatus.STRONG_ORIENTATION) {
            message = "Current extracted evidence supports using "
                    + formatCount(evidence.flowCount(), "representative flow")
                    + " as orientation patterns without currently surfaced ambiguity, low-confidence, or hotspot pressure.";
        } else {
            List<String> softeners = new ArrayList<>();
            if (evidence.ambiguousFlowCount() > 0) {
                softeners.add("ambiguity in " + formatCount(evidence.ambiguousFlowCount(), "flow"));
            }
            if (evidence.lowConfidenceFlowCount() > 0) {
                softeners.add("low confidence in " + formatCount(evidence.lowConfidenceFlowCount(), "flow"));
            }
            if (evidence.hotspotFlowCount() > 0) {
                softeners.add("legacy hotspot pressure in " + formatCount(evidence.hotspotFlowCount(), "flow"));
            }
            if (evidence.multiPurposeEntryPointCount() > 0) {
                softeners.add(formatCount(evidence.multiPurposeEntryPointCount(), "multi-purpose entry point"));
            }
            message = "Representative flows remain usable for orientation, but "
                    + joinWithAnd(softeners)
                    + " means some paths should be treated as provisional rather than canonical.";
        }

        return new GuidanceSignal("Representative flow trust", status, message);
    }

    private GuidanceSignal buildCautionSignal(GuidanceEvidence evidence) {
        if (evidence.hotspotFlowCount() == 0
                && evidence.ambiguousFlowCount() == 0
                && evidence.multiPurposeEntryPointCount() == 0) {
            return new GuidanceSignal(
                    "Caution-heavy areas",
                    GuidanceSignalStatus.NOT_APPLICABLE,
                    "Current extracted evidence does not surface a distinct caution-heavy representative area."
            );
        }

        List<String> cautionSources = new ArrayList<>();
        if (evidence.hotspotFlowCount() > 0) {
            cautionSources.add("legacy hotspot signals in " + formatCount(evidence.hotspotFlowCount(), "flow"));
        }
        if (evidence.ambiguousFlowCount() > 0) {
            cautionSources.add("residual ambiguity in " + formatCount(evidence.ambiguousFlowCount(), "flow"));
        }
        if (evidence.multiPurposeEntryPointCount() > 0) {
            cautionSources.add(formatCount(evidence.multiPurposeEntryPointCount(), "multi-purpose entry point"));
        }

        return new GuidanceSignal(
                "Caution-heavy areas",
                GuidanceSignalStatus.CAUTION,
                "Legacy or mixed-entry signals suggest extra caution around "
                        + evidence.dominantRolePattern()
                        + " variants touched by "
                        + joinWithAnd(cautionSources)
                        + "."
        );
    }

    private GuidanceSignal buildRulePolicySignal(GuidanceEvidence evidence) {
        GuidanceSignalStatus status;
        String message;

        if (evidence.rulesLoadedCount() == 0) {
            status = GuidanceSignalStatus.PROVISIONAL;
            message = "Rule and policy signals should be treated as secondary guidance because project rule input is not currently loaded.";
        } else if (evidence.policySnapshot().overallStatus() == ProjectPolicyStatus.MIXED
                || evidence.driftRuleSignals() > 0) {
            status = GuidanceSignalStatus.CAUTION;
            message = "Rule and policy signals suggest a mixed posture, so they should guide caution rather than architectural verdicts.";
        } else if (evidence.policySnapshot().overallStatus() == ProjectPolicyStatus.ALIGNED
                && evidence.alignedRuleSignals() > 0) {
            status = GuidanceSignalStatus.STRONG_ORIENTATION;
            message = "Current rule and policy signals support using the observed representative layering as orientation, while still treating it as extracted evidence.";
        } else if (evidence.notApplicableRuleSignals() > 0) {
            status = GuidanceSignalStatus.NOT_APPLICABLE;
            message = "Current extracted evidence does not make the loaded rule set broadly applicable to representative flows.";
        } else {
            status = GuidanceSignalStatus.PROVISIONAL;
            message = "Policy posture remains limited, so rule signals should be read as soft guidance rather than final structure claims.";
        }

        return new GuidanceSignal("Rule and policy posture", status, message);
    }

    private GuidanceSignal buildUnknownSignal(GuidanceEvidence evidence) {
        if (evidence.unknownAffinityCount() == 0) {
            return new GuidanceSignal(
                    "Unknown-heavy areas",
                    GuidanceSignalStatus.NOT_APPLICABLE,
                    "Current extracted evidence does not surface unknown-affinity pressure."
            );
        }

        GuidanceSignalStatus status = isUnknownHeavy(evidence)
                ? GuidanceSignalStatus.CAUTION
                : GuidanceSignalStatus.PROVISIONAL;
        return new GuidanceSignal(
                "Unknown-heavy areas",
                status,
                "Unknown-affinity areas should be treated as mapping gaps, not proven structural defects. Current extracted evidence still includes "
                        + formatCount(evidence.unknownAffinityCount(), "unknown-affinity file")
                        + "."
        );
    }

    private List<String> buildSummaryLines(GuidanceEvidence evidence) {
        List<String> lines = new ArrayList<>();
        GuidanceSignal orientationSignal = buildOrientationSignal(evidence);
        GuidanceSignal trustSignal = buildTrustSignal(evidence);
        GuidanceSignal rulePolicySignal = buildRulePolicySignal(evidence);

        lines.add(orientationSignal.message());
        lines.add(trustSignal.message());

        if (isCautionHeavy(evidence)) {
            lines.add("Residual ambiguity means some flows should be treated as provisional rather than canonical.");
        } else {
            lines.add(rulePolicySignal.message());
        }

        if (isUnknownHeavy(evidence)) {
            lines.add("Unknown-heavy areas should be treated as mapping gaps, not proven structural defects.");
        }

        return lines.stream()
                .limit(4)
                .toList();
    }

    private List<String> buildNotes(GuidanceEvidence evidence) {
        List<String> notes = new ArrayList<>();
        notes.add("Guidance is based on current extracted evidence and should not be read as proven codebase truth.");
        notes.add("Representative flows remain orientation aids, not runtime guarantees.");

        if (evidence.policySnapshot().overallStatus() == ProjectPolicyStatus.WEAK_SIGNAL
                || evidence.policySnapshot().overallStatus() == ProjectPolicyStatus.NOT_APPLICABLE) {
            notes.add("Weak or not-applicable policy posture should soften guidance rather than override raw evidence.");
        } else if (isCautionHeavy(evidence) || isUnknownHeavy(evidence)) {
            notes.add("Mixed, hotspot-sensitive, or unknown-heavy areas should be read cautiously even when orientation paths look useful.");
        }

        return notes.stream()
                .limit(3)
                .toList();
    }

    private GuidanceSignalStatus determineOrientationStatus(GuidanceEvidence evidence) {
        if (evidence.flowCount() == 0) {
            return GuidanceSignalStatus.NOT_APPLICABLE;
        }
        return evidence.ambiguousFlowCount() == 0
                && evidence.lowConfidenceFlowCount() == 0
                && evidence.hotspotFlowCount() == 0
                && evidence.multiPurposeEntryPointCount() == 0
                ? GuidanceSignalStatus.STRONG_ORIENTATION
                : GuidanceSignalStatus.PROVISIONAL;
    }

    private GuidanceSignalStatus determineTrustStatus(GuidanceEvidence evidence) {
        if (evidence.flowCount() == 0) {
            return GuidanceSignalStatus.NOT_APPLICABLE;
        }
        return isCautionHeavy(evidence)
                ? GuidanceSignalStatus.PROVISIONAL
                : GuidanceSignalStatus.STRONG_ORIENTATION;
    }

    private boolean isCautionHeavy(GuidanceEvidence evidence) {
        if (evidence.flowCount() == 0) {
            return false;
        }

        return evidence.hotspotFlowCount() > 0
                || evidence.multiPurposeEntryPointCount() > 0
                || ratio(evidence.ambiguousFlowCount(), evidence.flowCount()) >= 0.25
                || ratio(evidence.lowConfidenceFlowCount(), evidence.flowCount()) >= 0.25;
    }

    private boolean isUnknownHeavy(GuidanceEvidence evidence) {
        if (evidence.totalFileCount() == 0) {
            return false;
        }

        int threshold = Math.max(5, (int) Math.ceil(evidence.totalFileCount() * 0.15));
        return evidence.unknownAffinityCount() >= threshold;
    }

    private double ratio(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return numerator / (double) denominator;
    }

    private List<Map.Entry<String, Long>> sortByCountDescending(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.naturalOrder())))
                .toList();
    }

    private String startClassName(RepresentativeFlow flow) {
        List<String> classNames = flow.classNames();
        if (classNames == null || classNames.isEmpty()) {
            return "Unknown";
        }
        return normalize(classNames.get(0));
    }

    private String joinWithAnd(List<String> values) {
        List<String> normalized = values == null
                ? List.of()
                : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
        if (normalized.isEmpty()) {
            return "current extracted evidence";
        }
        if (normalized.size() == 1) {
            return normalized.get(0);
        }
        if (normalized.size() == 2) {
            return normalized.get(0) + " and " + normalized.get(1);
        }
        return String.join(", ", normalized.subList(0, normalized.size() - 1))
                + ", and "
                + normalized.get(normalized.size() - 1);
    }

    private String formatCount(int count, String singularNoun) {
        return count + " " + (count == 1 ? singularNoun : singularNoun + "s");
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }

    private record GuidanceEvidence(
            int totalFileCount,
            int flowCount,
            String dominantRolePattern,
            List<String> orientationStarts,
            int ambiguousFlowCount,
            int lowConfidenceFlowCount,
            int hotspotFlowCount,
            int multiPurposeEntryPointCount,
            int unknownAffinityCount,
            int rulesLoadedCount,
            int alignedRuleSignals,
            int driftRuleSignals,
            int notApplicableRuleSignals,
            ProjectPolicySnapshot policySnapshot
    ) {
    }
}
