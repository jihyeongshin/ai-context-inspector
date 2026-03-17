package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DependencyExtractor {

    private static final Set<String> EXCLUDED_SIMPLE_TYPES = Set.of(
            "String", "Object",
            "Integer", "Long", "Boolean", "Double", "Float", "Short", "Byte", "Character",
            "int", "long", "boolean", "double", "float", "short", "byte", "char",
            "List", "Set", "Map", "Collection", "Iterable",
            "Locale",
            "HttpServletRequest", "HttpServletResponse",
            "Logger"
    );

    public List<String> extract(PsiClass psiClass) {
        if (psiClass == null) {
            return Collections.emptyList();
        }

        List<String> dependencies = new ArrayList<>();

        for (PsiField field : psiClass.getFields()) {
            String typeName = extractSimpleTypeName(field.getType());
            if (typeName == null || typeName.isBlank()) {
                continue;
            }

            if (shouldExclude(typeName)) {
                continue;
            }

            dependencies.add(typeName);
        }

        return dependencies;
    }

    private String extractSimpleTypeName(PsiType psiType) {
        if (psiType == null) {
            return null;
        }

        String presentableText = psiType.getPresentableText();
        if (presentableText.isBlank()) {
            return null;
        }

        int genericStart = presentableText.indexOf('<');
        if (genericStart >= 0) {
            return presentableText.substring(0, genericStart).trim();
        }

        return presentableText.trim();
    }

    private boolean shouldExclude(String typeName) {
        if (EXCLUDED_SIMPLE_TYPES.contains(typeName)) {
            return true;
        }

        if (typeName.endsWith("Request") || typeName.endsWith("Response")) {
            return true;
        }

        if (typeName.endsWith("Exception")) {
            return true;
        }

        return false;
    }

}
