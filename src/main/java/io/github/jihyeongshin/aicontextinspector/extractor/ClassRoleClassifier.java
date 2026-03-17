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

        // 1.Annotation 기반
        String annotationRole = classifyByAnnotation(psiClass);
        if (annotationRole != null) {
            return annotationRole;
        }

        // 2.Enum special case
        if (psiClass.isEnum()) {
            return "Enum";
        }

        // 3.Class name 기반
        String nameRole = classifyByClassName(className);
        if (nameRole != null) {
            return nameRole;
        }

        // 4.Package 기반
        String packageRole = classifyByPackage(packageName);
        if (packageRole != null) {
            return packageRole;
        }

        // 5.Test detection
        if (className.endsWith("Test") || className.endsWith("Tests")) {
            return "Test";
        }

        return "Unknown";
    }

    private String classifyByAnnotation(PsiClass psiClass) {

        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList == null) {
            return null;
        }

        for (PsiAnnotation annotation : modifierList.getAnnotations()) {

            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }

            if (qualifiedName.endsWith("RestController")
                    || qualifiedName.endsWith("Controller")) {
                return "Controller";
            }

            if (qualifiedName.endsWith("Service")) {
                return "Service";
            }

            if (qualifiedName.endsWith("Repository")) {
                return "Repository";
            }

            if (qualifiedName.endsWith("Entity")) {
                return "Entity";
            }

            if (qualifiedName.endsWith("Configuration")) {
                return "Config";
            }

            if (qualifiedName.endsWith("ControllerAdvice")
                    || qualifiedName.endsWith("RestControllerAdvice")) {
                return "Advice";
            }
        }

        return null;
    }

    private String classifyByClassName(String className) {

        if (className.endsWith("Controller")) return "Controller";
        if (className.endsWith("Facade")) return "Facade";
        if (className.endsWith("UseCase")) return "UseCase";
        if (className.endsWith("Service")) return "Service";
        if (className.endsWith("Repository")) return "Repository";

        if (className.endsWith("Entity")) return "Entity";

        if (className.endsWith("Request")) return "RequestDTO";
        if (className.endsWith("Response")) return "ResponseDTO";

        if (className.endsWith("Dto") || className.endsWith("DTO")) return "DTO";

        if (className.endsWith("Event")) return "Event";

        if (className.endsWith("Exception")) return "Exception";

        if (className.endsWith("Config")
                || className.endsWith("Configuration")) {
            return "Config";
        }

        if (className.endsWith("History")) return "History";

        if (className.endsWith("Paginator")
                || className.endsWith("PageRequest")
                || className.endsWith("PageResponse")) {
            return "Paginator";
        }

        return null;
    }

    private String classifyByPackage(String packageName) {

        if (packageName == null || packageName.isBlank()) {
            return null;
        }

        if (packageName.contains(".controller.")) return "Controller";
        if (packageName.contains(".facade.")) return "Facade";
        if (packageName.contains(".usecase.")) return "UseCase";
        if (packageName.contains(".service.")) return "Service";
        if (packageName.contains(".repository.")) return "Repository";

        if (packageName.contains(".request.")) return "RequestDTO";
        if (packageName.contains(".response.")) return "ResponseDTO";
        if (packageName.contains(".dto.")) return "DTO";

        if (packageName.contains(".event.")) return "Event";

        if (packageName.contains(".exception.")) return "Exception";

        if (packageName.contains(".config.")) return "Config";

        if (packageName.contains(".history.")) return "History";

        if (packageName.contains(".paging.")
                || packageName.contains(".paginator.")) {
            return "Paginator";
        }

        return null;
    }

}