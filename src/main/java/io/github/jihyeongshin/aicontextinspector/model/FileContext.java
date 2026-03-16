package io.github.jihyeongshin.aicontextinspector.model;

import com.intellij.psi.PsiClass;

public record FileContext(
        String projectName,
        String moduleName,
        String fileName,
        String filePath,
        String packageName,
        PsiClass primaryClass,
        String primaryClassName
) {
}
