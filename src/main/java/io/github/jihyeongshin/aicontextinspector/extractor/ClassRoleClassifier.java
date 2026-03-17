package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierList;
import io.github.jihyeongshin.aicontextinspector.model.ClassClassification;

public class ClassRoleClassifier {

    public ClassClassification classify(PsiClass psiClass, String packageName) {

        String className = psiClass.getName();
        if (className == null) {
            return new ClassClassification("Unknown", "None");
        }

        String springStereotype = classifySpringStereotype(psiClass);

        if (psiClass.isEnum()) {
            return new ClassClassification("Enum", springStereotype);
        }

        String classRole = classifyClassRoleByName(className);
        if (classRole == null) {
            classRole = classifyClassRoleByPackage(packageName);
        }
        if (classRole == null) {
            classRole = classifyClassRoleByAnnotation(psiClass);
        }
        if (classRole == null) {
            if (className.endsWith("Test") || className.endsWith("Tests")) {
                classRole = "Test";
            } else {
                classRole = "Unknown";
            }
        }

        return new ClassClassification(classRole, springStereotype);
    }

    private String classifySpringStereotype(PsiClass psiClass) {
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList == null) {
            return "None";
        }

        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }

            if (qualifiedName.endsWith(".RestController")) return "RestController";
            if (qualifiedName.endsWith(".Controller")) return "Controller";
            if (qualifiedName.endsWith(".Service")) return "Service";
            if (qualifiedName.endsWith(".Repository")) return "Repository";
            if (qualifiedName.endsWith(".Component")) return "Component";
            if (qualifiedName.endsWith(".Configuration")) return "Configuration";
            if (qualifiedName.endsWith(".RestControllerAdvice")) return "RestControllerAdvice";
            if (qualifiedName.endsWith(".ControllerAdvice")) return "ControllerAdvice";
        }

        return "None";
    }

    private String classifyClassRoleByName(String className) {
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
        if (className.endsWith("Config") || className.endsWith("Configuration")) return "Config";
        if (className.endsWith("History")) return "History";
        if (className.endsWith("Paginator")
                || className.endsWith("PageRequest")
                || className.endsWith("PageResponse")) return "Paginator";

        return null;
    }

    private String classifyClassRoleByPackage(String packageName) {
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
        if (packageName.contains(".paging.") || packageName.contains(".paginator.")) return "Paginator";

        return null;
    }

    private String classifyClassRoleByAnnotation(PsiClass psiClass) {
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList == null) {
            return null;
        }

        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }

            if (qualifiedName.endsWith(".RestController") || qualifiedName.endsWith(".Controller")) {
                return "Controller";
            }
            if (qualifiedName.endsWith(".Repository")) {
                return "Repository";
            }
            if (qualifiedName.endsWith(".Entity")) {
                return "Entity";
            }
            if (qualifiedName.endsWith(".Configuration")) {
                return "Config";
            }
            if (qualifiedName.endsWith(".ControllerAdvice")
                    || qualifiedName.endsWith(".RestControllerAdvice")) {
                return "Advice";
            }
        }

        return null;
    }


}