package io.github.jihyeongshin.aicontextinspector.model.source;

import java.util.List;

public record ClassClassification(
        String classRole,
        String architectureAffinity,
        String springStereotype,
        List<String> structuralTraits
) {
}
