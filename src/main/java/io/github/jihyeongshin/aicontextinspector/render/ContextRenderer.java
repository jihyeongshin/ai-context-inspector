package io.github.jihyeongshin.aicontextinspector.render;

import io.github.jihyeongshin.aicontextinspector.model.ContextSnapshot;

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
                Fields: %s
                Methods: %s
                Endpoints: %s
                Dependencies: %s
                """.formatted(
                snapshot.projectName(),
                snapshot.moduleName(),
                snapshot.fileName(),
                snapshot.filePath(),
                snapshot.packageName(),
                snapshot.className(),
                snapshot.classType(),
                snapshot.annotations(),
                snapshot.imports(),
                snapshot.fields(),
                snapshot.methods(),
                snapshot.endpoints(),
                snapshot.dependencies()
        );
    }
}
