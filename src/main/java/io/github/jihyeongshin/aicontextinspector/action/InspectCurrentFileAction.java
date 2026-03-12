package io.github.jihyeongshin.aicontextinspector.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class InspectCurrentFileAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

        Project project = anActionEvent.getProject();
        if (project == null) return;

        VirtualFile virtualFile = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            Messages.showWarningDialog(project, "No file is selected", "AI Context Inspector");
            return;
        }

        String message = """
                Project: %s
                File: %s
                Path: %s
                """.formatted(
                project.getName(),
                virtualFile.getName(),
                virtualFile.getPath()
        );
        Messages.showInfoMessage(project, message, "AI Context Inspector");

    }

}
