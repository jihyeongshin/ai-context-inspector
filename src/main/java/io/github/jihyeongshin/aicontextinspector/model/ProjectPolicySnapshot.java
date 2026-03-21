package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;

public record ProjectPolicySnapshot(
        ProjectPolicyStatus overallStatus,
        ProjectPolicyEvidence evidence,
        List<ProjectPolicySignal> signals,
        List<ProjectPolicyCaution> cautions
) {
    public ProjectPolicySnapshot {
        overallStatus = overallStatus == null ? ProjectPolicyStatus.NOT_APPLICABLE : overallStatus;
        evidence = evidence == null ? new ProjectPolicyEvidence(0, 0, 0, 0, 0, 0, 0) : evidence;
        signals = signals == null ? List.of() : List.copyOf(signals);
        cautions = cautions == null ? List.of() : List.copyOf(cautions);
    }

    public String overallPostureDisplayString() {
        if (overallStatus == ProjectPolicyStatus.ALIGNED && !cautions.isEmpty()) {
            return "Mostly aligned";
        }
        return overallStatus.displayName();
    }
}
