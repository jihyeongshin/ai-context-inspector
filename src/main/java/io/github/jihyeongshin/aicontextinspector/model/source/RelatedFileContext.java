package io.github.jihyeongshin.aicontextinspector.model.source;

import java.util.List;

public record RelatedFileContext(
        String className,
        String classRole,
        String springStereotype,
        String filePath,
        String packageName,
        String relationType,
        List<String> dependencies,
        int score
) {
}
