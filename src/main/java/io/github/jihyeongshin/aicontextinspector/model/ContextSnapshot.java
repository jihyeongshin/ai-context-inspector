package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;

public record ContextSnapshot(
        String projectName,
        String moduleName,
        String fileName,
        String filePath,
        String packageName,
        String className,
        String classType,
        List<String> annotations,
        List<String> imports
) {

}
