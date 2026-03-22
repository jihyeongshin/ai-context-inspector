package io.github.jihyeongshin.aicontextinspector.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.application.index.ProjectContextIndexer;
import io.github.jihyeongshin.aicontextinspector.render.debug.ProjectContextDebugRenderer;
import io.github.jihyeongshin.aicontextinspector.ui.ContextPreviewDialog;
import org.jetbrains.annotations.NotNull;

public class PreviewProjectContextAction extends AnAction {

    private final ProjectContextIndexer projectContextIndexer = new ProjectContextIndexer();
    private final ProjectContextDebugRenderer renderer = new ProjectContextDebugRenderer();

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

        String message = renderer.render(snapshot);
        new ContextPreviewDialog(project, message).show();
    }
}
