package io.github.jihyeongshin.aicontextinspector.extraction.discovery;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ProjectJavaFileFinder {

    public List<PsiJavaFile> findAll(Project project) {
        if (project == null) {
            return List.of();
        }

        return ReadAction.compute(() -> {
            List<PsiJavaFile> result = new ArrayList<>();
            PsiManager psiManager = PsiManager.getInstance(project);
            Collection<VirtualFile> virtualFiles =
                    FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));
            for (VirtualFile virtualFile : virtualFiles) {
                if (!shouldInclude(virtualFile)) {
                    continue;
                }
                PsiFile psiFile = psiManager.findFile(virtualFile);
                if (!(psiFile instanceof PsiJavaFile javaFile)) {
                    continue;
                }
                if (javaFile.getClasses().length == 0) {
                    continue;
                }
                result.add(javaFile);
            }
            result.sort(
                    Comparator.comparing(psiJavaFile -> {
                        VirtualFile virtualFile = psiJavaFile.getVirtualFile();
                        return virtualFile != null ? virtualFile.getPath() : psiJavaFile.getName();
                    })
            );
            return result;
        });
    }

    private boolean shouldInclude(VirtualFile virtualFile) {
        if (virtualFile == null || virtualFile.isDirectory()) {
            return false;
        }
        String path = virtualFile.getPath().replace('\\', '/');
        if (path.contains("/build/generated/")) {
            return false;
        }

        if (path.contains("/out/")) {
            return false;
        }

        // /src/main/test/* exclude test classes not yet implemented

        return true;
    }
}
