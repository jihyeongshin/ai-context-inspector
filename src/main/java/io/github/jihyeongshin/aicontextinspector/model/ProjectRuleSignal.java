package io.github.jihyeongshin.aicontextinspector.model;

public record ProjectRuleSignal(
        String ruleId,
        ProjectRuleSignalStatus status,
        int alignedFlows,
        int driftFlows,
        int applicableFlows,
        String summary
) {
    public ProjectRuleSignal {
        ruleId = ruleId == null || ruleId.isBlank() ? "Unknown" : ruleId.trim();
        status = status == null ? ProjectRuleSignalStatus.NOT_APPLICABLE : status;
        alignedFlows = Math.max(alignedFlows, 0);
        driftFlows = Math.max(driftFlows, 0);
        applicableFlows = Math.max(applicableFlows, 0);
        summary = summary == null || summary.isBlank()
                ? status.displayName()
                : summary.trim();
    }
}
