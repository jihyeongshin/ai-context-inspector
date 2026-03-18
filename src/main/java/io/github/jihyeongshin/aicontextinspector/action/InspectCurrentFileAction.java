package io.github.jihyeongshin.aicontextinspector.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import io.github.jihyeongshin.aicontextinspector.extractor.JavaContextExtractor;
import io.github.jihyeongshin.aicontextinspector.extractor.RelatedContextCollector;
import io.github.jihyeongshin.aicontextinspector.extractor.RelatedFlowCollector;
import io.github.jihyeongshin.aicontextinspector.model.ContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.RelatedFileContext;
import io.github.jihyeongshin.aicontextinspector.model.RelatedFlow;
import io.github.jihyeongshin.aicontextinspector.render.ContextRenderer;
import io.github.jihyeongshin.aicontextinspector.ui.ContextPreviewDialog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InspectCurrentFileAction extends AnAction {

    private final JavaContextExtractor extractor = new JavaContextExtractor();
    private final RelatedContextCollector relatedContextCollector = new RelatedContextCollector();
    private final ContextRenderer renderer = new ContextRenderer();
    RelatedFlowCollector relatedFlowCollector = new RelatedFlowCollector();

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

        Project project = anActionEvent.getProject();
        VirtualFile virtualFile = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null) {
            return;
        }

        if (virtualFile == null) {
            Messages.showWarningDialog(project, "No file is selected.", "AI Context Inspector");
            return;
        }

        // virtualFile to PsiFile
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (!(psiFile instanceof PsiJavaFile javaFile)) {
            Messages.showWarningDialog(
                    project,
                    "The selected file is not a Java file.",
                    "AI Context Inspector"
            );
            return;
        }

        PsiClass[] classes = javaFile.getClasses();
        PsiClass sourceClass = classes.length > 0 ? classes[0] : null;

        ContextSnapshot snapshot = extractor.extract(project, javaFile, virtualFile);
        List<RelatedFileContext> relatedFiles = relatedContextCollector.collect(project, javaFile, sourceClass);
        List<RelatedFlow> relatedFlows = relatedFlowCollector.collect(project, javaFile, sourceClass);

        String message = renderer.render(snapshot, relatedFiles, relatedFlows);

        new ContextPreviewDialog(project, message).show();

    }

}
