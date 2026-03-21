package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;

public record ProjectRule(
        String id,
        ProjectRuleKind kind,
        String description,
        String fromAffinity,
        List<String> toAffinityAnyOf,
        List<String> affinityAnyOf,
        ProjectRuleLevel level
) {
    public ProjectRule {
        id = normalize(id);
        description = description == null ? "" : description.trim();
        fromAffinity = normalizeNullable(fromAffinity);
        toAffinityAnyOf = sanitizeList(toAffinityAnyOf);
        affinityAnyOf = sanitizeList(affinityAnyOf);
        level = level == null ? ProjectRuleLevel.GUIDE : level;
    }

    private static List<String> sanitizeList(List<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                .map(ProjectRule::normalizeNullable)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
