package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import io.github.jihyeongshin.aicontextinspector.model.ContextSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JavaContextExtractor {

    private final ClassRoleClassifier classRoleClassifier = new ClassRoleClassifier();

    public ContextSnapshot extract(Project project, PsiJavaFile javaFile, VirtualFile virtualFile) {
        String projectName = project.getName();
        String moduleName = resolveModuleName(javaFile, virtualFile);
        String fileName = virtualFile.getName();
        String filePath = virtualFile.getPath();
        String packageName = javaFile.getPackageName();

        // get top level class
        PsiClass[] classes = javaFile.getClasses();
        PsiClass primaryClass = classes.length > 0 ? classes[0] : null;

        String className = primaryClass != null && primaryClass.getName() != null
                ? primaryClass.getName()
                : "N/A";

        String classType = primaryClass != null
                ? classRoleClassifier.classify(primaryClass, packageName)
                : "Unknown";

        List<String> annotations = primaryClass != null
                ? extractAnnotations(primaryClass)
                : Collections.emptyList();

        List<String> imports = extractImports(javaFile);

        return new ContextSnapshot(
                projectName,
                moduleName,
                fileName,
                filePath,
                packageName,
                className,
                classType,
                annotations,
                imports
        );
    }

    private String resolveModuleName(PsiJavaFile javaFile, VirtualFile virtualFile) {
        Module module = ModuleUtilCore.findModuleForPsiElement(javaFile);
        if (module == null && virtualFile != null) {
            module = ModuleUtilCore.findModuleForFile(virtualFile, javaFile.getProject());
        }
        return module != null ? module.getName() : "Unknown";
    }

    private List<String> extractAnnotations(PsiClass psiClass) {
        if (psiClass.getModifierList() == null) {
            return Collections.emptyList();
        }

        List<String> annotations = new ArrayList<>();
        for (PsiAnnotation annotation : psiClass.getModifierList().getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null || qualifiedName.isBlank()) {
                continue;
            }

            annotations.add(toShortName(qualifiedName));
        }
        return annotations;
    }

    private List<String> extractImports(PsiJavaFile javaFile) {
        PsiImportList importList = javaFile.getImportList();
        if (importList == null) {
            return Collections.emptyList();
        }

        List<String> imports = new ArrayList<>();
        for (PsiImportStatementBase importStatement : importList.getAllImportStatements()) {
            String text = importStatement.getText();
            if (text == null || text.isBlank()) {
                continue;
            }

            text = text.replace("import ", "").replace(";", "").trim();
            imports.add(text);
        }
        return imports;
    }

    private String toShortName(String qualifiedName) {
        int lastDotIndex = qualifiedName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == qualifiedName.length() - 1) {
            return qualifiedName;
        }
        return qualifiedName.substring(lastDotIndex + 1);
    }


}
