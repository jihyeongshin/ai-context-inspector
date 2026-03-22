package io.github.jihyeongshin.aicontextinspector.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextExportResult;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.application.export.ProjectAiContextExporter;
import io.github.jihyeongshin.aicontextinspector.application.index.ProjectContextIndexer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class GenerateProjectAiContextAction extends AnAction {

    private final ProjectContextIndexer projectContextIndexer = new ProjectContextIndexer();
    private final ProjectAiContextExporter exporter = new ProjectAiContextExporter();

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }

        ProjectContextSnapshot snapshot = projectContextIndexer.index(project);
        if (snapshot.isEmpty()) {
            Messages.showWarningDialog(
                    project,
                    "No Java context could be indexed from this project.",
                    "AI Context Inspector"
            );
            return;
        }

        try {
            ProjectContextExportResult result = exporter.export(project, snapshot);
            Messages.showInfoMessage(
                    project,
                    buildSuccessMessage(result),
                    "AI Context Inspector"
            );
        } catch (IOException exception) {
            Messages.showErrorDialog(
                    project,
                    "Failed to generate project AI context.\n" + exception.getMessage(),
                    "AI Context Inspector"
            );
        }
    }

    private String buildSuccessMessage(ProjectContextExportResult result) {
        String files = result.files().stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.joining("\n- ", "- ", ""));

        return "Project AI context was generated.\n\n"
                + "Output directory:\n"
                + result.outputDirectory()
                + "\n\n"
                + "Files:\n"
                + files;
    }
}
