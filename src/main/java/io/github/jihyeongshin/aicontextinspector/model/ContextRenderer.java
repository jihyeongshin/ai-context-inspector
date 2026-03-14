package io.github.jihyeongshin.aicontextinspector.model;

public class ContextRenderer {

    public String render(ContextSnapshot snapshot) {
        return """
                Project: %s
                Module: %s
                File: %s
                FilePath: %s
                Package: %s
                Class: %s
                ClassType: %s
                Annotations: %s
                Imports: %s
                """.formatted(
                snapshot.projectName(),
                snapshot.moduleName(),
                snapshot.fileName(),
                snapshot.filePath(),
                snapshot.packageName(),
                snapshot.className(),
                snapshot.classType(),
                snapshot.annotations(),
                snapshot.imports()
        );
    }
}
