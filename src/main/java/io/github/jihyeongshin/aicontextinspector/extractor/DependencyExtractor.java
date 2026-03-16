package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;

import java.util.ArrayList;
import java.util.List;

public class DependencyExtractor {

    public List<String> extract(PsiClass psiClass) {
        List<String> dependencies = new ArrayList<>();
        for (PsiField field : psiClass.getFields()) {
            String typeName = field.getType().getPresentableText();

            if (isInfrastructureType(typeName)) {
                continue;
            }

            dependencies.add(typeName);
        }
        return dependencies;
    }

    private boolean isInfrastructureType(String typeName) {
        return typeName.startsWith("List<")
                || typeName.startsWith("Set<")
                || typeName.startsWith("Map<")
                || "String".equals(typeName)
                || "Long".equals(typeName)
                || "Integer".equals(typeName)
                || "Boolean".equals(typeName)
                || "int".equals(typeName)
                || "long".equals(typeName)
                || "boolean".equals(typeName);
    }

}
