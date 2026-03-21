package io.github.jihyeongshin.aicontextinspector.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import io.github.jihyeongshin.aicontextinspector.extractor.JavaContextExtractor;
import io.github.jihyeongshin.aicontextinspector.extractor.ProjectJavaFileFinder;
import io.github.jihyeongshin.aicontextinspector.model.ContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.ProjectRuleLoadResult;
import io.github.jihyeongshin.aicontextinspector.model.ProjectContextSnapshot;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProjectContextIndexer {

    private final ProjectJavaFileFinder projectJavaFileFinder = new ProjectJavaFileFinder();
    private final JavaContextExtractor javaContextExtractor = new JavaContextExtractor();
    private final ProjectRuleLoader projectRuleLoader = new ProjectRuleLoader();

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
        ProjectRuleLoadResult ruleLoadResult = projectRuleLoader.load(resolveProjectBasePath(project));
        return new ProjectContextSnapshot(
                snapshots,
                ruleLoadResult.ruleSet(),
                ruleLoadResult.ruleFileDetected(),
                ruleLoadResult.ruleSourcePath(),
                ruleLoadResult.rulesLoadedCount(),
                ruleLoadResult.supportedRuleKindsSummary(),
                ruleLoadResult.warnings()
        );
    }

    private Path resolveProjectBasePath(Project project) {
        String basePath = project == null ? null : project.getBasePath();
        return basePath == null || basePath.isBlank() ? null : Path.of(basePath);
    }
}
