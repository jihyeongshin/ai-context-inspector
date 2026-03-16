package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnnotationExtractor {

    public List<String> extract(PsiClass psiClass) {
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

    private String toShortName(String qualifiedName) {
        int lastDotIndex = qualifiedName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == qualifiedName.length() - 1) {
            return qualifiedName;
        }
        return qualifiedName.substring(lastDotIndex + 1);
    }

}
