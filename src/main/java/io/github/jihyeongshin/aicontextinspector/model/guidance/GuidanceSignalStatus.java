package io.github.jihyeongshin.aicontextinspector.model.guidance;

public enum GuidanceSignalStatus {
    STRONG_ORIENTATION("Qualified orientation"),
    CAUTION("Caution"),
    PROVISIONAL("Provisional"),
    NOT_APPLICABLE("Not applicable");

    private final String displayName;

    GuidanceSignalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
