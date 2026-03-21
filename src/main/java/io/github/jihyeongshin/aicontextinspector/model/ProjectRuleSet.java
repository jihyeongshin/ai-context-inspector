package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;

public record ProjectRuleSet(
        int version,
        List<ProjectRule> rules
) {
    public ProjectRuleSet {
        version = version <= 0 ? 1 : version;
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    public static ProjectRuleSet empty() {
        return new ProjectRuleSet(1, List.of());
    }
}
