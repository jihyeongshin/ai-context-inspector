package io.github.jihyeongshin.aicontextinspector.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import io.github.jihyeongshin.aicontextinspector.extractor.JavaContextExtractor;
import io.github.jihyeongshin.aicontextinspector.extractor.ProjectJavaFileFinder;
import io.github.jihyeongshin.aicontextinspector.model.ContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.ProjectContextSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProjectContextIndexer {

    private final ProjectJavaFileFinder projectJavaFileFinder = new ProjectJavaFileFinder();
    private final JavaContextExtractor javaContextExtractor = new JavaContextExtractor();

    public ProjectContextSnapshot index(Project project) {
        List<ContextSnapshot> snapshots = new ArrayList<>();
        List<PsiJavaFile> javaFiles = projectJavaFileFinder.findAll(project);
        for (PsiJavaFile javaFile : javaFiles) {
            VirtualFile virtualFile = javaFile.getVirtualFile();
            if (virtualFile == null) {
                continue;
            }
            ContextSnapshot snapshot = javaContextExtractor.extract(project, javaFile, virtualFile);
            if (snapshot == null) {
                continue;
            }
            snapshots.add(snapshot);
        }
        snapshots.sort(
                Comparator.comparing(
                        ContextSnapshot::filePath,
                        Comparator.nullsLast(String::compareTo)
                )
        );
        return new ProjectContextSnapshot(snapshots);
    }

}
