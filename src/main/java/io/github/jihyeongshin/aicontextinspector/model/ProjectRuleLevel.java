package io.github.jihyeongshin.aicontextinspector.model;

public enum ProjectRuleLevel {
    GUIDE("guide"),
    WARN("warn");

    private final String yamlValue;

    ProjectRuleLevel(String yamlValue) {
        this.yamlValue = yamlValue;
    }

    public String yamlValue() {
        return yamlValue;
    }

    public static ProjectRuleLevel fromYamlValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (ProjectRuleLevel level : values()) {
            if (level.yamlValue.equals(value)) {
                return level;
            }
        }
        return null;
    }
}
