package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;

public record RepresentativeFlow(
        List<String> classNames,
        List<String> classRoles,
        int score
) {
    public RepresentativeFlow {
        classNames = classNames == null ? List.of() : List.copyOf(classNames);
        classRoles = classRoles == null ? List.of() : List.copyOf(classRoles);
    }

    public String toDisplayString() {
        return String.join(" -> ", classNames);
    }

    public String toRoleDisplayString() {
        return String.join(" -> ", classRoles);
    }
}
