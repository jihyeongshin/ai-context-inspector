package io.github.jihyeongshin.aicontextinspector.model.flow;

public enum EntryPointInterpretation {
    FOCUSED("Focused"),
    MULTI_PURPOSE("Multi-purpose");

    private final String displayName;

    EntryPointInterpretation(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
