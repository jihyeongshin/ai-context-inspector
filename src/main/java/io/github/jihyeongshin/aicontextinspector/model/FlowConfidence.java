package io.github.jihyeongshin.aicontextinspector.model;

public enum FlowConfidence {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    private final String displayName;

    FlowConfidence(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
