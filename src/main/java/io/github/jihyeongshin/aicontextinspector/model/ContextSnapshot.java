package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;

public record ContextSnapshot(
        String projectName,
        String moduleName,
        String fileName,
        String filePath,
        String packageName,
        String className,
        String classRole,
        String architectureAffinity,
        String springStereotype,
        List<String> structuralTraits,
        List<String> annotations,
        List<String> imports,
        List<String> fields,
        List<String> methods,
        List<String> endpoints,
        List<String> dependencies
) {

}
