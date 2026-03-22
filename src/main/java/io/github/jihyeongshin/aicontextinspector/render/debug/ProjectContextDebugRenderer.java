package io.github.jihyeongshin.aicontextinspector.render.debug;

import io.github.jihyeongshin.aicontextinspector.model.flow.EntryPointInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.InterpretedRepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowAmbiguityInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowEntryPointInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowLegacyHotspotInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.policy.ProjectPolicyCaution;
import io.github.jihyeongshin.aicontextinspector.model.policy.ProjectPolicyEvidence;
import io.github.jihyeongshin.aicontextinspector.model.policy.ProjectPolicySnapshot;
import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRuleSignal;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.source.ContextSnapshot;

import io.github.jihyeongshin.aicontextinspector.analysis.flow.RepresentativeFlowBuilder;
import io.github.jihyeongshin.aicontextinspector.analysis.flow.RepresentativeFlowAmbiguityInterpreter;
import io.github.jihyeongshin.aicontextinspector.analysis.flow.RepresentativeFlowEntryPointInterpreter;
import io.github.jihyeongshin.aicontextinspector.analysis.flow.RepresentativeFlowLegacyHotspotInterpreter;
import io.github.jihyeongshin.aicontextinspector.analysis.flow.RepresentativeFlowMetadataEvaluator;
import io.github.jihyeongshin.aicontextinspector.analysis.policy.ProjectPolicyEvaluator;
import io.github.jihyeongshin.aicontextinspector.analysis.rule.ProjectRuleEvaluator;

import java.util.*;
import java.util.stream.Collectors;

public class ProjectContextDebugRenderer {
    private static final int UNKNOWN_ROLE_SAMPLE_LIMIT_PER_GROUP = 20;
    private static final int REPRESENTATIVE_FLOW_LIMIT = 20;
    private static final List<String> TARGETED_ENTRY_POINT_TOKENS = List.of("Root", "Chat", "User");

    private final RepresentativeFlowBuilder representativeFlowBuilder = new RepresentativeFlowBuilder();
    private final RepresentativeFlowAmbiguityInterpreter representativeFlowAmbiguityInterpreter =
            new RepresentativeFlowAmbiguityInterpreter();
    private final RepresentativeFlowEntryPointInterpreter representativeFlowEntryPointInterpreter =
            new RepresentativeFlowEntryPointInterpreter();
    private final RepresentativeFlowLegacyHotspotInterpreter representativeFlowLegacyHotspotInterpreter =
            new RepresentativeFlowLegacyHotspotInterpreter();
    private final RepresentativeFlowMetadataEvaluator representativeFlowMetadataEvaluator =
            new RepresentativeFlowMetadataEvaluator();
    private final ProjectRuleEvaluator projectRuleEvaluator = new ProjectRuleEvaluator();
    private final ProjectPolicyEvaluator projectPolicyEvaluator = new ProjectPolicyEvaluator();

    public String render(ProjectContextSnapshot projectSnapshot) {
        List<ContextSnapshot> files = projectSnapshot.files();

        StringBuilder sb = new StringBuilder();
        sb.append("Project AI Context Index Summary").append("\n\n");
        sb.append("Total Files: ").append(files.size()).append("\n");
        sb.append("Endpoint Files: ").append(countEndpointFiles(files)).append("\n\n");

        appendRoleCounts(sb, files);
        appendTopDependencyFiles(sb, files);
        appendSampleFiles(sb, files);
        appendAffinityCounts(sb, files);
        appendAffinitySamples(sb, files);
        appendTraitCounts(sb, files);
        appendTraitSamples(sb, files);
        appendProjectRuleInput(sb, projectSnapshot);
        appendProjectRuleSignals(sb, projectSnapshot);
        appendProjectPolicySummary(sb, projectSnapshot);
        appendRepresentativeFlows(sb, projectSnapshot);
        appendTargetedEntryPointInterpretationChecks(sb, projectSnapshot);
        appendUnknownRoleSamples(sb, files);
        appendUnknownAffinitySamples(sb, files);

        return sb.toString();
    }

    private int countEndpointFiles(List<ContextSnapshot> files) {
        int count = 0;
        for (ContextSnapshot file : files) {
            if (file.endpoints() != null && !file.endpoints().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void appendRoleCounts(StringBuilder sb, List<ContextSnapshot> files) {
        sb.append("Role Counts").append("\n");

        Map<String, Long> counts = files.stream()
                .collect(Collectors.groupingBy(
                        file -> normalize(file.classRole()),
                        TreeMap::new,
                        Collectors.counting()
                ));

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            sb.append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendProjectRuleInput(StringBuilder sb, ProjectContextSnapshot projectSnapshot) {
        sb.append("Project Rule Input").append("\n");
        sb.append("- Rule file detected: ")
                .append(projectSnapshot.ruleFileDetected() ? "Yes" : "No")
                .append("\n");
        sb.append("- Rule source: ")
                .append(normalize(projectSnapshot.ruleSourcePath()))
                .append("\n");
        sb.append("- Rules loaded: ")
                .append(projectSnapshot.rulesLoadedCount())
                .append("\n");
        sb.append("- Load warnings: ")
                .append(projectSnapshot.ruleLoadWarnings().size())
                .append("\n");
        sb.append("- Supported rule kinds: ")
                .append(projectSnapshot.supportedRuleKindsSummary().isEmpty()
                        ? "None"
                        : String.join(", ", projectSnapshot.supportedRuleKindsSummary()))
                .append("\n\n");
    }

    private void appendProjectRuleSignals(StringBuilder sb, ProjectContextSnapshot projectSnapshot) {
        sb.append("Project Rule Signals").append("\n");

        boolean hasSignals = false;
        for (String warning : projectSnapshot.ruleLoadWarnings()) {
            sb.append("- Load warning: ").append(warning).append("\n");
            hasSignals = true;
        }

        if (!projectSnapshot.hasProjectRules()) {
            if (!hasSignals) {
                sb.append("- No project rules loaded").append("\n\n");
                return;
            }
            sb.append("\n");
            return;
        }

        List<ProjectRuleSignal> signals = projectRuleEvaluator.evaluate(
                projectSnapshot,
                representativeFlowBuilder.build(projectSnapshot)
        );
        if (signals.isEmpty()) {
            if (!hasSignals) {
                sb.append("- No rule signals available").append("\n\n");
                return;
            }
            sb.append("\n");
            return;
        }

        for (ProjectRuleSignal signal : signals) {
            sb.append("- ")
                    .append(signal.ruleId())
                    .append(": ")
                    .append(signal.summary())
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendProjectPolicySummary(StringBuilder sb, ProjectContextSnapshot projectSnapshot) {
        sb.append("Project Policy Summary").append("\n");

        List<RepresentativeFlow> flows = representativeFlowBuilder.build(projectSnapshot);
        ProjectPolicySnapshot policySnapshot = projectPolicyEvaluator.evaluate(projectSnapshot, flows);
        Map<String, Long> signalCounts = projectPolicyEvaluator.countSignalsByStatus(policySnapshot);

        sb.append("- Overall policy posture: ")
                .append(policySnapshot.overallPostureDisplayString())
                .append("\n");
        sb.append("- Evidence base: ")
                .append(formatCount(policySnapshot.evidence().representativeFlowCount(), "representative flow"))
                .append(", ")
                .append(formatCount(policySnapshot.evidence().ingestedRuleCount(), "ingested rule"))
                .append(", ")
                .append(formatCount(policySnapshot.evidence().distinctMultiPurposeEntryPoints(), "distinct multi-purpose entry point"))
                .append("\n");
        sb.append("- Signal count by status: ")
                .append(signalCounts.isEmpty() ? "None" : formatTopSummary(signalCounts))
                .append("\n");
        sb.append("- Caution summary: ")
                .append(formatPolicyCautionSummary(policySnapshot))
                .append("\n\n");
    }

    private void appendTopDependencyFiles(StringBuilder sb, List<ContextSnapshot> files) {
        sb.append("Top Dependency Files").append("\n");

        List<ContextSnapshot> topFiles = files.stream()
                .sorted(Comparator
                        .comparingInt((ContextSnapshot file) -> safeList(file.dependencies()).size())
                        .reversed()
                        .thenComparing(ContextSnapshot::fileName, Comparator.nullsLast(String::compareTo)))
                .limit(5)
                .toList();

        for (ContextSnapshot file : topFiles) {
            sb.append("- ")
                    .append(normalize(file.fileName()))
                    .append(" | deps=")
                    .append(safeList(file.dependencies()).size())
                    .append(" | role=")
                    .append(normalize(file.classRole()))
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendSampleFiles(StringBuilder sb, List<ContextSnapshot> files) {
        sb.append("Sample Files").append("\n");

        List<ContextSnapshot> sampleFiles = files.stream()
                .sorted(Comparator.comparing(ContextSnapshot::filePath, Comparator.nullsLast(String::compareTo)))
                .limit(10)
                .toList();

        for (ContextSnapshot file : sampleFiles) {
            sb.append("- ")
                    .append(normalize(file.fileName()))
                    .append(" | ")
                    .append(normalize(file.classRole()))
                    .append(" | ")
                    .append(normalize(file.packageName()))
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendAffinityCounts(StringBuilder sb, List<ContextSnapshot> files) {
        sb.append("Affinity Counts").append("\n");

        Map<String, Long> counts = files.stream()
                .collect(Collectors.groupingBy(
                        file -> normalize(file.architectureAffinity()),
                        TreeMap::new,
                        Collectors.counting()
                ));

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            sb.append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendAffinitySamples(StringBuilder sb, List<ContextSnapshot> files) {
        sb.append("Affinity Samples").append("\n");

        Map<String, List<String>> samples = files.stream()
                .collect(Collectors.groupingBy(
                        file -> normalize(file.architectureAffinity()),
                        TreeMap::new,
                        Collectors.mapping(
                                file -> normalize(file.className()),
                                Collectors.toList()
                        )
                ));

        for (Map.Entry<String, List<String>> entry : samples.entrySet()) {
            List<String> topSamples = entry.getValue().stream()
                    .distinct()
                    .sorted()
                    .limit(5)
                    .toList();

            sb.append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(topSamples)
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendTraitCounts(StringBuilder sb, List<ContextSnapshot> files) {
        sb.append("Trait Counts").append("\n");

        Map<String, Long> counts = new TreeMap<>();

        for (ContextSnapshot file : files) {
            for (String trait : safeList(file.structuralTraits())) {
                String normalized = normalize(trait);
                counts.put(normalized, counts.getOrDefault(normalized, 0L) + 1);
            }
        }

        if (counts.isEmpty()) {
            sb.append("- None").append("\n\n");
            return;
        }

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            sb.append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendTraitSamples(StringBuilder sb, List<ContextSnapshot> files) {
        sb.append("Trait Samples").append("\n");

        Map<String, List<String>> samples = new TreeMap<>();

        for (ContextSnapshot file : files) {
            for (String trait : safeList(file.structuralTraits())) {
                String normalizedTrait = normalize(trait);
                samples.computeIfAbsent(normalizedTrait, key -> new ArrayList<>())
                        .add(normalize(file.className()));
            }
        }

        if (samples.isEmpty()) {
            sb.append("- None").append("\n\n");
            return;
        }

        for (Map.Entry<String, List<String>> entry : samples.entrySet()) {
            List<String> topSamples = entry.getValue().stream()
                    .distinct()
                    .sorted()
                    .limit(5)
                    .toList();

            sb.append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(topSamples)
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendUnknownRoleSamples(StringBuilder sb, List<ContextSnapshot> files) {
        List<ContextSnapshot> unknownRoleFiles = files.stream()
                .filter(file ->
                        "Unknown".equals(normalize(file.classRole()))
                )
                .toList();

        sb.append("Unclassified Role Samples")
                .append(" (")
                .append(unknownRoleFiles.size())
                .append(")")
                .append("\n");

        if (unknownRoleFiles.isEmpty()) {
            sb.append("- None").append("\n\n");
            return;
        }

        Map<UnknownRoleGroup, List<ContextSnapshot>> groupedUnknowns = createUnknownRoleGroups();

        for (ContextSnapshot unknownRoleFile : unknownRoleFiles) {
            groupedUnknowns.get(classifyUnknownRoleGroup(unknownRoleFile)).add(unknownRoleFile);
        }

        for (UnknownRoleGroup group : UnknownRoleGroup.values()) {
            appendUnknownRoleGroup(sb, group, groupedUnknowns.get(group));
        }
    }

    private void appendUnknownAffinitySamples(StringBuilder sb, List<ContextSnapshot> files) {
        sb.append("Unclassified Affinity Samples").append("\n");

        List<String> unknowns = files.stream()
                .filter(file ->
                        "Unknown".equals(normalize(file.architectureAffinity()))
                )
                .map(file -> normalize(file.className()))
                .distinct()
                .sorted()
                .limit(100)
                .toList();

        if (unknowns.isEmpty()) {
            sb.append("- None").append("\n\n");
            return;
        }

        for (String unknown : unknowns) {
            sb.append("- ").append(unknown).append("\n");
        }
        sb.append("\n");
    }

    private void appendRepresentativeFlows(StringBuilder sb, ProjectContextSnapshot projectSnapshot) {
        sb.append("Representative Flows").append("\n");

        List<RepresentativeFlow> flows = representativeFlowBuilder.build(projectSnapshot);
        if (flows.isEmpty()) {
            sb.append("- None").append("\n\n");
            return;
        }

        List<InterpretedRepresentativeFlow> interpretedFlows =
                representativeFlowMetadataEvaluator.evaluate(projectSnapshot, flows);
        Map<String, RepresentativeFlowEntryPointInterpretation> entryPointInterpretations =
                representativeFlowEntryPointInterpreter.evaluate(projectSnapshot, flows);
        Map<String, RepresentativeFlowAmbiguityInterpretation> ambiguityInterpretations =
                representativeFlowAmbiguityInterpreter.evaluate(projectSnapshot, interpretedFlows);
        Map<String, RepresentativeFlowLegacyHotspotInterpretation> legacyHotspotInterpretations =
                representativeFlowLegacyHotspotInterpreter.evaluate(projectSnapshot, flows);
        List<InterpretedRepresentativeFlow> topFlows = interpretedFlows.stream()
                .limit(REPRESENTATIVE_FLOW_LIMIT)
                .toList();

        for (InterpretedRepresentativeFlow interpretedFlow : topFlows) {
            RepresentativeFlow flow = interpretedFlow.flow();
            RepresentativeFlowEntryPointInterpretation entryPointInterpretation = entryPointInterpretations.getOrDefault(
                    representativeFlowEntryPointInterpreter.entryPointIdentity(projectSnapshot, flow),
                    new RepresentativeFlowEntryPointInterpretation(null, List.of())
            );
            RepresentativeFlowLegacyHotspotInterpretation legacyHotspotInterpretation = legacyHotspotInterpretations.getOrDefault(
                    flow.toDisplayString(),
                    new RepresentativeFlowLegacyHotspotInterpretation(null, List.of())
            );
            RepresentativeFlowAmbiguityInterpretation ambiguityInterpretation = ambiguityInterpretations.getOrDefault(
                    flow.toDisplayString(),
                    new RepresentativeFlowAmbiguityInterpretation(null, List.of())
            );
            sb.append("- ")
                    .append(flow.toDisplayString())
                    .append(" | roles=")
                    .append(flow.toRoleDisplayString())
                    .append(" | score=")
                    .append(flow.score())
                    .append(" | confidence=")
                    .append(interpretedFlow.metadata().confidence().displayName())
                    .append(" | ambiguity=")
                    .append(interpretedFlow.metadata().ambiguity().displayName())
                    .append(" | ambiguity-notes=")
                    .append(ambiguityInterpretation.notesDisplayString())
                    .append(" | notes=")
                    .append(interpretedFlow.metadata().notesDisplayString())
                    .append(" | entry-point=")
                    .append(entryPointInterpretation.interpretation().displayName())
                    .append(" | interpretation-notes=")
                    .append(entryPointInterpretation.notesDisplayString())
                    .append(" | legacy-hotspot=")
                    .append(legacyHotspotInterpretation.legacyHotspot().displayName())
                    .append(" | hotspot-notes=")
                    .append(legacyHotspotInterpretation.hotspotNotesDisplayString())
                    .append("\n");
        }

        if (interpretedFlows.size() > topFlows.size()) {
            sb.append("- ... and ")
                    .append(interpretedFlows.size() - topFlows.size())
                    .append(" more")
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendTargetedEntryPointInterpretationChecks(StringBuilder sb, ProjectContextSnapshot projectSnapshot) {
        sb.append("Targeted Entry Point Interpretation Checks").append("\n");

        List<RepresentativeFlow> flows = representativeFlowBuilder.build(projectSnapshot);
        if (flows.isEmpty()) {
            sb.append("- None").append("\n\n");
            return;
        }

        List<InterpretedRepresentativeFlow> interpretedFlows =
                representativeFlowMetadataEvaluator.evaluate(projectSnapshot, flows);
        Map<String, RepresentativeFlowEntryPointInterpretation> entryPointInterpretations =
                representativeFlowEntryPointInterpreter.evaluate(projectSnapshot, flows);
        Map<String, RepresentativeFlowAmbiguityInterpretation> ambiguityInterpretations =
                representativeFlowAmbiguityInterpreter.evaluate(projectSnapshot, interpretedFlows);

        List<InterpretedRepresentativeFlow> targetedFlows = interpretedFlows.stream()
                .filter(interpretedFlow -> matchesTargetedEntryPoint(interpretedFlow.flow()))
                .toList();

        if (targetedFlows.isEmpty()) {
            sb.append("- None").append("\n\n");
            return;
        }

        for (InterpretedRepresentativeFlow interpretedFlow : targetedFlows) {
            RepresentativeFlow flow = interpretedFlow.flow();
            RepresentativeFlowEntryPointInterpretation entryPointInterpretation = entryPointInterpretations.getOrDefault(
                    representativeFlowEntryPointInterpreter.entryPointIdentity(projectSnapshot, flow),
                    new RepresentativeFlowEntryPointInterpretation(null, List.of())
            );
            RepresentativeFlowAmbiguityInterpretation ambiguityInterpretation = ambiguityInterpretations.getOrDefault(
                    flow.toDisplayString(),
                    new RepresentativeFlowAmbiguityInterpretation(null, List.of())
            );
            sb.append("- ")
                    .append(flow.toDisplayString())
                    .append(" | entry-point=")
                    .append(entryPointInterpretation.interpretation().displayName())
                    .append(" | interpretation-notes=")
                    .append(entryPointInterpretation.notesDisplayString())
                    .append(" | confidence=")
                    .append(interpretedFlow.metadata().confidence().displayName())
                    .append(" | ambiguity=")
                    .append(interpretedFlow.metadata().ambiguity().displayName())
                    .append(" | ambiguity-notes=")
                    .append(ambiguityInterpretation.notesDisplayString())
                    .append("\n");
        }
        sb.append("\n");
    }

    private boolean matchesTargetedEntryPoint(RepresentativeFlow flow) {
        String startClassName = startClassKey(flow);
        if ("Unknown".equals(startClassName)) {
            return false;
        }

        for (String token : TARGETED_ENTRY_POINT_TOKENS) {
            if (startClassName.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String startClassKey(RepresentativeFlow flow) {
        List<String> classNames = safeList(flow.classNames());
        if (classNames.isEmpty()) {
            return "Unknown";
        }
        return normalize(classNames.get(0));
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private void appendUnknownRoleGroup(StringBuilder sb, UnknownRoleGroup group, List<ContextSnapshot> files) {
        sb.append(group.title())
                .append(" (")
                .append(files.size())
                .append(")")
                .append("\n");

        List<String> samples = files.stream()
                .map(file -> normalize(file.className()))
                .distinct()
                .sorted()
                .limit(UNKNOWN_ROLE_SAMPLE_LIMIT_PER_GROUP)
                .toList();

        if (samples.isEmpty()) {
            sb.append("- None").append("\n\n");
            return;
        }

        for (String sample : samples) {
            sb.append("- ").append(sample).append("\n");
        }

        if (files.size() > samples.size()) {
            sb.append("- ... and ")
                    .append(files.size() - samples.size())
                    .append(" more")
                    .append("\n");
        }
        sb.append("\n");
    }

    private Map<UnknownRoleGroup, List<ContextSnapshot>> createUnknownRoleGroups() {
        Map<UnknownRoleGroup, List<ContextSnapshot>> groups = new LinkedHashMap<>();
        for (UnknownRoleGroup group : UnknownRoleGroup.values()) {
            groups.put(group, new ArrayList<>());
        }
        return groups;
    }

    private UnknownRoleGroup classifyUnknownRoleGroup(ContextSnapshot file) {
        if ("DataLike".equals(normalize(file.architectureAffinity()))) {
            return UnknownRoleGroup.DATA_LIKE_OR_ENTITY_BACKED;
        }

        boolean isEntityBacked = safeList(file.structuralTraits()).stream()
                .map(this::normalize)
                .anyMatch("EntityBackedLike"::equals);

        if (isEntityBacked) {
            return UnknownRoleGroup.DATA_LIKE_OR_ENTITY_BACKED;
        }

        if ("SupportLike".equals(normalize(file.architectureAffinity()))) {
            return UnknownRoleGroup.SUPPORT_LIKE;
        }

        return UnknownRoleGroup.TRULY_UNKNOWN;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }

    private String formatTopSummary(Map<String, Long> counts) {
        if (counts.isEmpty()) {
            return "None";
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
    }

    private String formatPolicyCautionSummary(ProjectPolicySnapshot policySnapshot) {
        List<String> parts = new ArrayList<>();
        ProjectPolicyEvidence evidence = policySnapshot.evidence();

        if (policySnapshot.cautions().contains(ProjectPolicyCaution.RULE_INPUT_MISSING)) {
            parts.add("rule input missing");
        }
        if (policySnapshot.cautions().contains(ProjectPolicyCaution.AMBIGUITY_PRESENT)) {
            parts.add("ambiguity present in " + formatCount(evidence.ambiguousFlowCount(), "flow"));
        }
        if (policySnapshot.cautions().contains(ProjectPolicyCaution.HOTSPOT_PRESENT)) {
            parts.add("legacy hotspot present in " + formatCount(evidence.hotspotFlowCount(), "flow"));
        }
        if (policySnapshot.cautions().contains(ProjectPolicyCaution.LOW_CONFIDENCE_PRESENT)) {
            parts.add("low confidence present in " + formatCount(evidence.lowConfidenceFlowCount(), "flow"));
        }
        if (policySnapshot.cautions().contains(ProjectPolicyCaution.UNKNOWN_AFFINITY_PRESENT)) {
            parts.add("unknown affinity present in " + formatCount(evidence.unknownAffinityCount(), "file"));
        }

        return parts.isEmpty() ? "None" : String.join(", ", parts);
    }

    private String formatCount(long count, String singularNoun) {
        return count + " " + (count == 1 ? singularNoun : singularNoun + "s");
    }

    private enum UnknownRoleGroup {
        DATA_LIKE_OR_ENTITY_BACKED("Unclassified Roles - Data-like or Entity-backed"),
        SUPPORT_LIKE("Unclassified Roles - Support-like"),
        TRULY_UNKNOWN("Unclassified Roles - Other patterns");

        private final String title;

        UnknownRoleGroup(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }
}

