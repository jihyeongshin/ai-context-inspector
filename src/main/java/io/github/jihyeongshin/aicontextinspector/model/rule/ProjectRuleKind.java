package io.github.jihyeongshin.aicontextinspector.model.rule;

public enum ProjectRuleKind {
    EXPECTED_TRANSITION("expected_transition"),
    PREFERRED_TERMINAL_AFFINITY("preferred_terminal_affinity"),
    DISCOURAGED_MID_AFFINITY("discouraged_mid_affinity");

    private final String yamlValue;

    ProjectRuleKind(String yamlValue) {
        this.yamlValue = yamlValue;
    }

    public String yamlValue() {
        return yamlValue;
    }

    public static ProjectRuleKind fromYamlValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (ProjectRuleKind kind : values()) {
            if (kind.yamlValue.equals(value)) {
                return kind;
            }
        }
        return null;
    }
}
