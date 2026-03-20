package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;

public record ClassClassification(
        String classRole,
        String architectureAffinity,
        String springStereotype,
        List<String> structuralTraits
) {
}
