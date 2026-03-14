package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierList;

public class ClassRoleClassifier {

    public String classify(PsiClass psiClass, String packageName) {
        String className = psiClass.getName();
        if (className == null) {
            return "Unknown";
        }
        if (hasAnnotation(psiClass, "RestController") || hasAnnotation(psiClass, "Controller")) {
            return "Controller";
        }

        if (className.endsWith("Controller")) {
            return "Controller";
        }

        if (className.endsWith("Facade")) {
            return "Facade";
        }
        if (className.endsWith("UseCase")) {
            return "UseCase";
        }
        if (packageName.endsWith(".request") || packageName.contains(".request.")) {
            return "Request DTO";
        }

        if (packageName.endsWith(".response") || packageName.contains(".response.")) {
            return "Response DTO";
        }
        if (className.endsWith("Service")) {
            return "Service";
        }

        if (className.endsWith("Repository")) {
            return "Repository";
        }

        return "Unknown";
    }

    private boolean hasAnnotation(PsiClass psiClass, String shortName) {
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList == null) {
            return false;
        }

        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }

            if (qualifiedName.equals(shortName) || qualifiedName.endsWith("." + shortName)) {
                return true;
            }
        }

        return false;
    }

}
