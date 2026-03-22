package io.github.jihyeongshin.aicontextinspector.model.policy;

public enum ProjectPolicyStatus {
    ALIGNED("Aligned"),
    MIXED("Mixed"),
    WEAK_SIGNAL("Weak signal"),
    NOT_APPLICABLE("Not applicable");

    private final String displayName;

    ProjectPolicyStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
