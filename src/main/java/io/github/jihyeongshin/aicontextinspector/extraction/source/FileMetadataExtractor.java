package io.github.jihyeongshin.aicontextinspector.extraction.source;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import io.github.jihyeongshin.aicontextinspector.model.source.FileContext;

public class FileMetadataExtractor {

    public FileContext extract(Project project, PsiJavaFile javaFile, VirtualFile virtualFile) {
        Module module = ModuleUtilCore.findModuleForPsiElement(javaFile);
        if (module == null && virtualFile != null) {
            module = ModuleUtilCore.findModuleForFile(virtualFile, javaFile.getProject());
        }
        String projectName = project.getName();
        String moduleName = module != null ? module.getName() : "Unknown";
        String fileName = virtualFile.getName();
        String filePath = virtualFile.getPath();
        String packageName = javaFile.getPackageName();

        PsiClass[] classes = javaFile.getClasses();
        PsiClass primaryClass = classes.length > 0 ? classes[0] : null;
        String primaryClassName = primaryClass != null && primaryClass.getName() != null ?
                primaryClass.getName() :
                "Unknown";

        return new FileContext(
                projectName,
                moduleName,
                fileName,
                filePath,
                packageName,
                primaryClass,
                primaryClassName
        );

    }

}

