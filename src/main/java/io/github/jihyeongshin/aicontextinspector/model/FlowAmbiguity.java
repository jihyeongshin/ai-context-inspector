package io.github.jihyeongshin.aicontextinspector.model;

public enum FlowAmbiguity {
    NONE("None"),
    POSSIBLE("Possible"),
    HIGH("High");

    private final String displayName;

    FlowAmbiguity(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
