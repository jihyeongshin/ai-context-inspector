package io.github.jihyeongshin.aicontextinspector.render;

import io.github.jihyeongshin.aicontextinspector.model.ContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.RelatedFileContext;
import io.github.jihyeongshin.aicontextinspector.model.RelatedFlow;

import java.util.List;

public class ContextRenderer {

    public String render(ContextSnapshot snapshot) {
        return """
                Project: %s
                Module: %s
                File: %s
                FilePath: %s
                Package: %s
                Class: %s
                ClassRole: %s
                SpringStereoType: %s
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
                snapshot.classRole(),
                snapshot.springStereotype(),
                snapshot.annotations(),
                snapshot.imports(),
                snapshot.fields(),
                snapshot.methods(),
                snapshot.endpoints(),
                snapshot.dependencies()
        );
    }

    public String render(ContextSnapshot snapshot, List<RelatedFileContext> relatedFiles, List<RelatedFlow> relatedFlows) {
        StringBuilder sb = new StringBuilder();
        sb.append(render(snapshot));

        sb.append("RelatedFiles:\n");
        if (relatedFiles == null || relatedFiles.isEmpty()) {
            sb.append("[]\n");
            return sb.toString();
        }

        for (RelatedFileContext related : relatedFiles) {
            sb.append("- ")
                    .append(related.relationType())
                    .append(" | ")
                    .append(related.classRole())
                    .append(" | ")
                    .append(related.springStereotype())
                    .append(" | ")
                    .append(related.className())
                    .append(" | ")
                    .append(related.packageName())
                    .append(" | ")
                    .append(related.filePath())
                    .append(" | dependencies=")
                    .append(related.dependencies())
                    .append("\n");
        }

        sb.append("RelatedFlows:\n");
        if (relatedFlows == null || relatedFlows.isEmpty()) {
            sb.append("[]\n");
        } else {
            for (RelatedFlow flow : relatedFlows) {
                sb.append("- ")
                        .append(flow.toDisplayString())
                        .append(" | score=")
                        .append(flow.score())
                        .append("\n");
            }
        }

        return sb.toString();
    }
}
