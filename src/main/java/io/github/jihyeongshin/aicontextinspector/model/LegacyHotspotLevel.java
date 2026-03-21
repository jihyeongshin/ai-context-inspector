package io.github.jihyeongshin.aicontextinspector.model;

public enum LegacyHotspotLevel {
    NONE("None"),
    POSSIBLE("Possible"),
    HIGH("High");

    private final String displayName;

    LegacyHotspotLevel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
