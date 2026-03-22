package io.github.jihyeongshin.aicontextinspector.extraction.classification;

import com.intellij.psi.*;
import io.github.jihyeongshin.aicontextinspector.model.source.ClassClassification;

import java.util.ArrayList;
import java.util.List;

public class ClassRoleClassifier {

    public ClassClassification classify(PsiClass psiClass, String packageName) {

        String className = psiClass.getName();
        if (className == null) {
            return new ClassClassification("Unknown", "Unknown", "None", List.of());
        }

        String springStereotype = classifySpringStereotype(psiClass);

        if (psiClass.isEnum()) {
            return new ClassClassification("Enum", "DataLike", springStereotype, List.of());
        }

        String classRole = classifyClassRoleByName(className);
        if (classRole == null) {
            classRole = classifyClassRoleByPackage(packageName);
        }
        if (classRole == null) {
            classRole = classifyClassRoleByAnnotation(psiClass);
        }
        if (classRole == null) {
            classRole = classifyConstantHolder(psiClass, className);
        }
        if (classRole == null) {
            if (className.endsWith("Test") || className.endsWith("Tests")) {
                classRole = "Test";
            } else {
                classRole = "Unknown";
            }
        }

        String architectureAffinity =
                classifyArchitectureAffinity(psiClass, packageName, className, classRole, springStereotype);

        List<String> structuralTraits = classifyStructuralTraits(psiClass, className, classRole, architectureAffinity);
        architectureAffinity = refineUnknownAffinityByTraits(architectureAffinity, structuralTraits);
        return new ClassClassification(classRole, architectureAffinity, springStereotype, structuralTraits);
    }

    private String classifyArchitectureAffinity(
            PsiClass psiClass,
            String packageName,
            String className,
            String classRole,
            String springStereotype
    ) {
        if (classRole == null) {
            return "Unknown";
        }

        switch (classRole) {
            case "Controller":
            case "Advice":
                return "EntryPointLike";

            case "Facade":
            case "UseCase":
            case "Service":
                return "ApplicationLike";

            case "Repository":
            case "Mapper":
                return "PersistenceLike";

            case "Client":
            case "Adapter":
            case "Dispatcher":
            case "Gateway":
            case "Bucket":
                return "AdapterLike";

            case "Entity":
            case "DTO":
            case "RequestDTO":
            case "ResponseDTO":
            case "Model":
            case "Message":
            case "Template":
            case "Event":
            case "Enum":
            case "Payload":
            case "QueryResult":
            case "Result":
            case "Metadata":
            case "Scope":
            case "Condition":
            case "Task":
            case "Context":
            case "Spec":
            case "Command":
            case "Cursor":
            case "Widget":
                return "DataLike";

            case "Config":
            case "Util":
            case "Factory":
            case "ConstantHolder":
            case "History":
            case "Paginator":
            case "Handler":
            case "Provider":
            case "Properties":
            case "Application":
            case "Processor":
            case "Filter":
            case "Logger":
            case "Encoder":
            case "Cipher":
            case "Exception":
            case "Policy":
            case "Strategy":
            case "Accessor":
            case "Generator":
            case "Validator":
                return "SupportLike";

            case "Test":
                return "TestLike";

            case "Unknown":
            default:
                break;
        }

        if ("RestController".equals(springStereotype) || "Controller".equals(springStereotype)) {
            return "EntryPointLike";
        }
        if ("Service".equals(springStereotype)) {
            return "ApplicationLike";
        }
        if ("Repository".equals(springStereotype)) {
            return "PersistenceLike";
        }
        if ("Configuration".equals(springStereotype) || "Component".equals(springStereotype)) {
            return "SupportLike";
        }

        if (packageName != null) {
            if (packageName.contains(".client.")
                    || packageName.contains(".external.")
                    || packageName.contains(".adapter.")
                    || packageName.contains(".openai.")
                    || packageName.contains(".alimtalk.")) {
                return "AdapterLike";
            }
            if (packageName.contains(".entity.")
                    || packageName.contains(".dto.")
                    || packageName.contains(".request.")
                    || packageName.contains(".response.")) {
                return "DataLike";
            }
            if (packageName.contains(".util.")
                    || packageName.contains(".config.")
                    || packageName.contains(".support.")
                    || packageName.contains(".paging.")
                    || packageName.contains(".paginator.")) {
                return "SupportLike";
            }
        }

        if (className != null) {
            if (className.endsWith("Client")
                    || className.endsWith("Adapter")
                    || className.endsWith("Dispatcher")
                    || className.endsWith("Gateway")) {
                return "AdapterLike";
            }
            if (className.endsWith("Request")
                    || className.endsWith("Response")
                    || className.endsWith("Dto")
                    || className.endsWith("DTO")
                    || className.endsWith("Entity")
                    || className.endsWith("Model")
                    || className.endsWith("Message")
                    || className.endsWith("Template")) {
                return "DataLike";
            }
            if (className.endsWith("Util")
                    || className.endsWith("Factory")
                    || className.endsWith("Constants")
                    || className.endsWith("Code")
                    || className.endsWith("Status")
                    || className.endsWith("Type")) {
                return "SupportLike";
            }
        }

        if (isSpringSecurityLike(psiClass, packageName)) {
            return "SupportLike";
        }

        return "Unknown";
    }

    private String classifyConstantHolder(PsiClass psiClass, String className) {
        PsiField[] fields = psiClass.getFields();
        if (fields.length == 0) {
            return null;
        }

        for (PsiField field : fields) {
            if (!(field.hasModifierProperty(PsiModifier.STATIC)
                    && field.hasModifierProperty(PsiModifier.FINAL))) {
                return null;
            }
        }

        int meaningfulMethodCount = 0;
        for (PsiMethod method : psiClass.getMethods()) {
            if (method.isConstructor()) {
                continue;
            }
            meaningfulMethodCount++;
        }

        if (meaningfulMethodCount > 0) {
            return null;
        }

        if (className.endsWith("Code")
                || className.endsWith("Type")
                || className.endsWith("Status")
                || className.endsWith("Constants")) {
            return "ConstantHolder";
        }

        return "ConstantHolder";
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

            if (qualifiedName.endsWith(".RestController") || qualifiedName.endsWith("RestController"))
                return "RestController";
            if (qualifiedName.endsWith(".Controller") || qualifiedName.endsWith("Controller"))
                return "Controller";
            if (qualifiedName.endsWith(".Service") || qualifiedName.endsWith("Service"))
                return "Service";
            if (qualifiedName.endsWith(".Repository") || qualifiedName.endsWith("Repository"))
                return "Repository";
            if (qualifiedName.endsWith(".Component") || qualifiedName.endsWith("Component"))
                return "Component";
            if (qualifiedName.endsWith(".Configuration") || qualifiedName.endsWith("Configuration"))
                return "Configuration";
            if (qualifiedName.endsWith(".RestControllerAdvice") || qualifiedName.endsWith("RestControllerAdvice"))
                return "RestControllerAdvice";
            if (qualifiedName.endsWith(".ControllerAdvice") || qualifiedName.endsWith("ControllerAdvice"))
                return "ControllerAdvice";
        }

        return "None";
    }

    private String classifyClassRoleByName(String className) {
        if (className.endsWith("Controller")) return "Controller";
        if (className.endsWith("Facade")) return "Facade";
        if (className.endsWith("UseCase")) return "UseCase";
        if (className.endsWith("Service")) return "Service";
        if (className.endsWith("Repository")) return "Repository";
        if (className.endsWith("Mapper")) return "Mapper";
        if (className.endsWith("Client")) return "Client";
        if (className.endsWith("Adapter")) return "Adapter";
        if (className.endsWith("Dispatcher")) return "Dispatcher";
        if (className.endsWith("Factory")) return "Factory";
        if (className.endsWith("Gateway")) return "Gateway";
        if (className.endsWith("Util")) return "Util";
        if (className.endsWith("Model")) return "Model";
        if (className.endsWith("Message")) return "Message";
        if (className.endsWith("Template")) return "Template";
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
        if (className.endsWith("Handler")) return "Handler";
        if (className.endsWith("Provider")) return "Provider";
        if (className.endsWith("Properties")) return "Properties";
        if (className.endsWith("Application")) return "Application";
        if (className.endsWith("Processor")) return "Processor";
        if (className.endsWith("Filter")) return "Filter";
        if (className.endsWith("Logger")) return "Logger";
        if (className.endsWith("Encoder")) return "Encoder";
        if (className.endsWith("Cipher")) return "Cipher";

        if (className.endsWith("Payload")) return "Payload";
        if (className.endsWith("QueryResult")) return "QueryResult";
        if (className.endsWith("Result")) return "Result";
        if (className.endsWith("Metadata")) return "Metadata";
        if (className.endsWith("Scope")) return "Scope";
        if (className.endsWith("Condition")) return "Condition";
        if (className.endsWith("Task")) return "Task";
        if (className.endsWith("Context")) return "Context";
        if (className.endsWith("Spec")) return "Spec";
        if (className.endsWith("Command")) return "Command";
        if (className.endsWith("Cursor")) return "Cursor";
        if (className.endsWith("Widget")) return "Widget";
        if (className.endsWith("Policy")) return "Policy";
        if (className.endsWith("Strategy")) return "Strategy";
        if (className.endsWith("Accessor")) return "Accessor";
        if (className.endsWith("Generator")) return "Generator";
        if (className.endsWith("Bucket")) return "Bucket";
        if (className.endsWith("Validator")) return "Validator";
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
        if (packageName.contains(".mapper.")) return "Mapper";
        if (packageName.contains(".client.")) return "Client";
        if (packageName.contains(".adapter.")) return "Adapter";
        if (packageName.contains(".request.")) return "RequestDTO";
        if (packageName.contains(".response.")) return "ResponseDTO";
        if (packageName.contains(".dto.")) return "DTO";
        if (packageName.contains(".event.")) return "Event";
        if (packageName.contains(".exception.")) return "Exception";
        if (packageName.contains(".config.")) return "Config";
        if (packageName.contains(".history.")) return "History";
        if (packageName.contains(".paging.") || packageName.contains(".paginator.")) return "Paginator";
        if (packageName.contains(".util.")) return "Util";

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

            if (qualifiedName.endsWith(".RestController")
                    || qualifiedName.endsWith("RestController")
                    || qualifiedName.endsWith(".Controller")
                    || qualifiedName.endsWith("Controller")) {
                return "Controller";
            }
            if (qualifiedName.endsWith(".Repository") || qualifiedName.endsWith("Repository")) {
                return "Repository";
            }
            if (qualifiedName.endsWith(".Entity") || qualifiedName.endsWith("Entity")) {
                return "Entity";
            }
            if (qualifiedName.endsWith(".Configuration") || qualifiedName.endsWith("Configuration")) {
                return "Config";
            }
            if (qualifiedName.endsWith(".ControllerAdvice")
                    || qualifiedName.endsWith("ControllerAdvice")
                    || qualifiedName.endsWith(".RestControllerAdvice")
                    || qualifiedName.endsWith("RestControllerAdvice")) {
                return "Advice";
            }
        }

        return null;
    }

    private boolean isEntityBacked(PsiClass psiClass) {
        return inheritsFromClassNameSuffix(psiClass, "Entity");
    }

    private boolean isBootstrapLike(String className) {
        return className != null && className.endsWith("Application");
    }

    private boolean isBehaviorRich(PsiClass psiClass) {
        int score = 0;
        for (PsiMethod method : psiClass.getMethods()) {
            String name = method.getName();
            if (name.startsWith("get") || name.startsWith("set")
                    || name.equals("toString") || name.equals("equals") || name.equals("hashCode")) {
                continue;
            }
            if (name.startsWith("can")
                    || name.startsWith("is")
                    || name.startsWith("start")
                    || name.startsWith("cancel")
                    || name.startsWith("assign")
                    || name.startsWith("calculate")
                    || name.startsWith("validate")
                    || name.startsWith("status")) {
                score += 2;
            } else {
                score += 1;
            }
        }
        return score >= 5;
    }

    private boolean isAnemicDataLike(PsiClass psiClass) {
        if (psiClass.getFields().length == 0) {
            return false;
        }

        int meaningfulMethodCount = 0;
        for (PsiMethod method : psiClass.getMethods()) {
            String name = method.getName();
            if (name.startsWith("get") || name.startsWith("set")
                    || name.equals("toString") || name.equals("equals") || name.equals("hashCode")) {
                continue;
            }
            if (method.isConstructor()) {
                continue;
            }
            meaningfulMethodCount++;
        }

        return meaningfulMethodCount <= 1;
    }

    private boolean isBucketLike(PsiClass psiClass, String className) {
        if (className != null && className.endsWith("Bucket")) {
            return true;
        }

        return inheritsFromClassNameSuffix(psiClass, "Bucket");
    }

    private boolean isSpringSecurityLike(PsiClass psiClass, String packageName) {
        if (packageName != null && packageName.contains(".springsecurity")) {
            return true;
        }

        return inheritsFromQualifiedNameContains(psiClass, "springframework.security");
    }

    private List<String> classifyStructuralTraits(
            PsiClass psiClass,
            String className,
            String classRole,
            String architectureAffinity
    ) {
        List<String> traits = new ArrayList<>();

        if (isBootstrapLike(className)) {
            traits.add("BootstrapLike");
        }

        if ("ConstantHolder".equals(classRole) || isConstantHolder(psiClass, className)) {
            traits.add("ConstantHolderLike");
        }

        if (!shouldApplyDataTraits(classRole, architectureAffinity)) {
            return traits;
        }

        if (isEntityBacked(psiClass)) {
            traits.add("EntityBackedLike");
        }

        if (isBucketLike(psiClass, className)) {
            traits.add("BucketLike");
        }

        boolean behaviorRich = isBehaviorRich(psiClass);
        if (behaviorRich) {
            traits.add("BehaviorRichLike");
        } else if (isAnemicDataLike(psiClass)) {
            traits.add("AnemicDataLike");
        }

        return traits;
    }

    private boolean shouldApplyDataTraits(String classRole, String architectureAffinity) {
        if (classRole == null) {
            return true;
        }

        switch (classRole) {
            case "Controller":
            case "Facade":
            case "UseCase":
            case "Service":
            case "Repository":
            case "Mapper":
            case "Handler":
            case "Filter":
            case "Provider":
            case "Logger":
            case "Processor":
            case "Config":
            case "Exception":
            case "Test":
                return false;
            default:
                break;
        }

        return "DataLike".equals(architectureAffinity) || "Unknown".equals(architectureAffinity);
    }

    private String refineUnknownAffinityByTraits(
            String currentAffinity,
            List<String> structuralTraits
    ) {
        if (!"Unknown".equals(currentAffinity)) {
            return currentAffinity;
        }

        if (structuralTraits.contains("ConstantHolderLike")) {
            return "SupportLike";
        }
        if (structuralTraits.contains("BootstrapLike")) {
            return "SupportLike";
        }
        if (structuralTraits.contains("BucketLike")) {
            return "AdapterLike";
        }
        if (structuralTraits.contains("BehaviorRichLike") && structuralTraits.contains("EntityBackedLike")) {
            return "DomainLike";
        }
        if (structuralTraits.contains("EntityBackedLike")) {
            return "DataLike";
        }
        if (structuralTraits.contains("AnemicDataLike")) {
            return "DataLike";
        }

        return "Unknown";
    }

    private boolean isConstantHolder(PsiClass psiClass, String className) {
        if (psiClass.isEnum()) {
            return false;
        }
        if (isThrowableLike(psiClass)) {
            return false;
        }
        if (!"None".equals(classifySpringStereotype(psiClass))) {
            return false;
        }

        PsiField[] fields = psiClass.getFields();
        if (fields.length == 0) {
            return false;
        }

        for (PsiField field : fields) {
            if (!(field.hasModifierProperty(PsiModifier.STATIC)
                    && field.hasModifierProperty(PsiModifier.FINAL))) {
                return false;
            }
        }

        int meaningfulMethodCount = 0;
        for (PsiMethod method : psiClass.getMethods()) {
            if (method.isConstructor()) {
                continue;
            }
            meaningfulMethodCount++;
        }

        if (meaningfulMethodCount > 0) {
            return false;
        }

        return className.endsWith("Code")
                || className.endsWith("Type")
                || className.endsWith("Status")
                || className.endsWith("Constants");
    }

    private boolean isThrowableLike(PsiClass psiClass) {
        PsiClass current = psiClass;
        while (current != null) {
            String qn = current.getQualifiedName();
            if ("java.lang.Throwable".equals(qn)) {
                return true;
            }
            current = current.getSuperClass();
        }
        return false;
    }

    private boolean inheritsFromClassNameSuffix(PsiClass psiClass, String suffix) {
        PsiClass current = psiClass.getSuperClass();
        while (current != null) {
            String name = current.getName();
            if (name != null && name.endsWith(suffix)) {
                return true;
            }
            current = current.getSuperClass();
        }
        return false;
    }

    private boolean inheritsFromQualifiedNameContains(PsiClass psiClass, String keyword) {
        PsiClass current = psiClass.getSuperClass();
        while (current != null) {
            String qn = current.getQualifiedName();
            if (qn != null && qn.contains(keyword)) {
                return true;
            }
            current = current.getSuperClass();
        }
        return false;
    }

}
