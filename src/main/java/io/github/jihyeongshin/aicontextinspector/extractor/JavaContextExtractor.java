package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import io.github.jihyeongshin.aicontextinspector.model.ClassClassification;
import io.github.jihyeongshin.aicontextinspector.model.ContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.FileContext;

import java.util.List;

public class JavaContextExtractor {

    AnnotationExtractor annotationExtractor = new AnnotationExtractor();
    DependencyExtractor dependencyExtractor = new DependencyExtractor();
    EndpointExtractor endpointExtractor = new EndpointExtractor();
    FieldExtractor fieldExtractor = new FieldExtractor();
    FileMetadataExtractor fileMetadataExtractor = new FileMetadataExtractor();
    ImportExtractor importExtractor = new ImportExtractor();
    MethodExtractor methodExtractor = new MethodExtractor();

    private final ClassRoleClassifier classRoleClassifier = new ClassRoleClassifier();

    public ContextSnapshot extract(Project project, PsiJavaFile javaFile, VirtualFile virtualFile) {
        FileContext fileContext = fileMetadataExtractor.extract(project, javaFile, virtualFile);
        PsiClass primaryClass = fileContext.primaryClass();
        if (primaryClass == null) {
            return new ContextSnapshot(
                    fileContext.projectName(),
                    fileContext.moduleName(),
                    fileContext.fileName(),
                    fileContext.filePath(),
                    fileContext.packageName(),
                    "Unknown",
                    "Unknown",
                    "Unknown",
                    List.of(),
                    importExtractor.extract(javaFile),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        ClassClassification classification = classRoleClassifier.classify(primaryClass, fileContext.packageName());

        return new ContextSnapshot(
                fileContext.projectName(),
                fileContext.moduleName(),
                fileContext.fileName(),
                fileContext.filePath(),
                fileContext.packageName(),
                fileContext.primaryClassName(),
                classification.classRole(),
                classification.springStereotype(),
                annotationExtractor.extract(primaryClass),
                importExtractor.extract(javaFile),
                fieldExtractor.extract(primaryClass),
                methodExtractor.extract(primaryClass),
                endpointExtractor.extract(primaryClass),
                dependencyExtractor.extract(primaryClass)
        );
    }

}
