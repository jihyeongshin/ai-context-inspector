package io.github.jihyeongshin.aicontextinspector.model;

public record ProjectPolicyEvidence(
        int representativeFlowCount,
        int ingestedRuleCount,
        int ambiguousFlowCount,
        int hotspotFlowCount,
        int lowConfidenceFlowCount,
        int unknownAffinityCount,
        int distinctMultiPurposeEntryPoints
) {
    public ProjectPolicyEvidence {
        representativeFlowCount = Math.max(representativeFlowCount, 0);
        ingestedRuleCount = Math.max(ingestedRuleCount, 0);
        ambiguousFlowCount = Math.max(ambiguousFlowCount, 0);
        hotspotFlowCount = Math.max(hotspotFlowCount, 0);
        lowConfidenceFlowCount = Math.max(lowConfidenceFlowCount, 0);
        unknownAffinityCount = Math.max(unknownAffinityCount, 0);
        distinctMultiPurposeEntryPoints = Math.max(distinctMultiPurposeEntryPoints, 0);
    }
}
