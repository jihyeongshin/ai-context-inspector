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
        List<String> fields = primaryClass != null ? extractFields(primaryClass) : Collections.emptyList();
        List<String> methods = primaryClass != null ? extractMethods(primaryClass) : Collections.emptyList();
        List<String> endpoints = primaryClass != null ? extractEndpoints(primaryClass) : Collections.emptyList();
        List<String> dependencies = primaryClass != null ? extractDependencies(primaryClass) : Collections.emptyList();

        return new ContextSnapshot(
                projectName,
                moduleName,
                fileName,
                filePath,
                packageName,
                className,
                classType,
                annotations,
                imports,
                fields,
                methods,
                endpoints,
                dependencies
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

    private List<String> extractFields(PsiClass psiClass) {
        List<String> fields = new ArrayList<>();
        for (PsiField field : psiClass.getFields()) {
            String type = field.getType().getPresentableText();
            String name = field.getName();
            fields.add(type + " " + name);
        }
        return fields;
    }

    private List<String> extractMethods(PsiClass psiClass) {
        List<String> methods = new ArrayList<>();
        for (PsiMethod method : psiClass.getMethods()) {
            if (method.isConstructor()) {
                continue;
            }

            String returnType = method.getReturnType() != null
                    ? method.getReturnType().getPresentableText()
                    : "void";

            StringBuilder params = new StringBuilder();
            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) {
                    params.append(", ");
                }
                params.append(parameters[i].getType().getPresentableText())
                        .append(" ")
                        .append(parameters[i].getName());
            }

            methods.add(method.getName() + "(" + params + "): " + returnType);
        }
        return methods;
    }

    private List<String> extractDependencies(PsiClass psiClass) {
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

    private List<String> extractEndpoints(PsiClass psiClass) {
        List<String> endpoints = new ArrayList<>();

        String basePath = extractClassRequestMapping(psiClass);

        for (PsiMethod method : psiClass.getMethods()) {
            String httpMethod = null;
            String subPath = "";

            for (PsiAnnotation annotation : method.getAnnotations()) {
                String qn = annotation.getQualifiedName();
                if (qn == null) {
                    continue;
                }

                if (qn.endsWith(".GetMapping")) {
                    httpMethod = "GET";
                    subPath = extractMappingValue(annotation);
                } else if (qn.endsWith(".PostMapping")) {
                    httpMethod = "POST";
                    subPath = extractMappingValue(annotation);
                } else if (qn.endsWith(".PutMapping")) {
                    httpMethod = "PUT";
                    subPath = extractMappingValue(annotation);
                } else if (qn.endsWith(".DeleteMapping")) {
                    httpMethod = "DELETE";
                    subPath = extractMappingValue(annotation);
                } else if (qn.endsWith(".PatchMapping")) {
                    httpMethod = "PATCH";
                    subPath = extractMappingValue(annotation);
                } else if (qn.endsWith(".RequestMapping")) {
                    httpMethod = "REQUEST";
                    subPath = extractMappingValue(annotation);
                }
            }

            if (httpMethod != null) {
                endpoints.add(httpMethod + " " + normalizePath(basePath, subPath) + " -> " + method.getName());
            }
        }

        return endpoints;
    }

    private String extractClassRequestMapping(PsiClass psiClass) {
        for (PsiAnnotation annotation : psiClass.getAnnotations()) {
            String qn = annotation.getQualifiedName();
            if (qn != null && qn.endsWith(".RequestMapping")) {
                return extractMappingValue(annotation);
            }
        }
        return "";
    }

    private String extractMappingValue(PsiAnnotation annotation) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
        if (value == null) {
            value = annotation.findAttributeValue("path");
        }
        if (value == null) {
            return "";
        }

        String text = value.getText();
        if (text == null) {
            return "";
        }

        return text.replace("\"", "").trim();
    }

    private String normalizePath(String basePath, String subPath) {
        String base = basePath == null ? "" : basePath.trim();
        String sub = subPath == null ? "" : subPath.trim();

        if (base.isEmpty() && sub.isEmpty()) {
            return "/";
        }
        if (base.isEmpty()) {
            return ensureSlash(sub);
        }
        if (sub.isEmpty()) {
            return ensureSlash(base);
        }
        return ensureSlash(base) + (sub.startsWith("/") ? sub : "/" + sub);
    }

    private String ensureSlash(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }


    private String toShortName(String qualifiedName) {
        int lastDotIndex = qualifiedName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == qualifiedName.length() - 1) {
            return qualifiedName;
        }
        return qualifiedName.substring(lastDotIndex + 1);
    }


}
