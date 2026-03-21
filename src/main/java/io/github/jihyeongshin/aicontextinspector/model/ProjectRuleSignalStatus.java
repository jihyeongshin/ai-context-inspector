package io.github.jihyeongshin.aicontextinspector.model;

public enum ProjectRuleSignalStatus {
    ALIGNED("Aligned"),
    POSSIBLE_DRIFT("Possible drift"),
    NOT_APPLICABLE("Not applicable"),
    LOAD_WARNING("Load warning");

    private final String displayName;

    ProjectRuleSignalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
