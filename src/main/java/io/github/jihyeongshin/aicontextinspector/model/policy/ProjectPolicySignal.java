package io.github.jihyeongshin.aicontextinspector.model.policy;

public record ProjectPolicySignal(
        String key,
        ProjectPolicyStatus status,
        String summary
) {
    public ProjectPolicySignal {
        key = key == null || key.isBlank() ? "Unknown" : key.trim();
        status = status == null ? ProjectPolicyStatus.WEAK_SIGNAL : status;
        summary = summary == null || summary.isBlank()
                ? status.displayName()
                : summary.trim();
    }
}
