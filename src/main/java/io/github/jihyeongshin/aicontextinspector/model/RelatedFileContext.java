package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;

public record RelatedFileContext(
        String className,
        String classType,
        String filePath,
        String packageName,
        String relationType,
        List<String> dependencies
) {
}
