package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.psi.*;
import io.github.jihyeongshin.aicontextinspector.model.EndpointInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EndpointExtractor {

    public List<String> extract(PsiClass psiClass) {
        if (psiClass == null) {
            return Collections.emptyList();
        }

        List<EndpointInfo> endpoints = extractEndpointInfos(psiClass);
        List<String> result = new ArrayList<>();

        for (EndpointInfo endpoint : endpoints) {
            result.add(endpoint.toDisplayString());
        }

        return result;
    }

    public List<EndpointInfo> extractEndpointInfos(PsiClass psiClass) {
        if (psiClass == null) {
            return Collections.emptyList();
        }

        List<EndpointInfo> result = new ArrayList<>();
        List<String> classPaths = extractClassLevelPaths(psiClass);

        for (PsiMethod method : psiClass.getMethods()) {
            EndpointMapping methodMapping = extractMethodLevelMapping(method);
            if (methodMapping == null) {
                continue;
            }

            List<String> methodPaths = methodMapping.paths().isEmpty()
                    ? List.of("")
                    : methodMapping.paths();

            for (String classPath : classPaths) {
                for (String methodPath : methodPaths) {
                    String normalizedPath = normalizePath(classPath, methodPath);

                    result.add(new EndpointInfo(
                            methodMapping.httpMethod(),
                            normalizedPath,
                            method.getName()
                    ));
                }
            }
        }

        return result;
    }

    private List<String> extractClassLevelPaths(PsiClass psiClass) {
        for (PsiAnnotation annotation : psiClass.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }

            if (qualifiedName.endsWith(".RequestMapping")) {
                List<String> paths = extractPaths(annotation);
                return paths.isEmpty() ? List.of("") : paths;
            }
        }

        return List.of("");
    }

    private EndpointMapping extractMethodLevelMapping(PsiMethod method) {
        for (PsiAnnotation annotation : method.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }

            if (qualifiedName.endsWith(".GetMapping") || qualifiedName.endsWith("GetMapping")) {
                return new EndpointMapping("GET", extractPaths(annotation));
            }

            if (qualifiedName.endsWith(".PostMapping") || qualifiedName.endsWith("PostMapping")) {
                return new EndpointMapping("POST", extractPaths(annotation));
            }

            if (qualifiedName.endsWith(".PutMapping") || qualifiedName.endsWith("PutMapping")) {
                return new EndpointMapping("PUT", extractPaths(annotation));
            }

            if (qualifiedName.endsWith(".DeleteMapping") || qualifiedName.endsWith("DeleteMapping")) {
                return new EndpointMapping("DELETE", extractPaths(annotation));
            }

            if (qualifiedName.endsWith(".PatchMapping") || qualifiedName.endsWith("PatchMapping")) {
                return new EndpointMapping("PATCH", extractPaths(annotation));
            }

            if (qualifiedName.endsWith(".RequestMapping") || qualifiedName.endsWith("RequestMapping")) {
                String httpMethod = extractRequestMethod(annotation);
                return new EndpointMapping(httpMethod, extractPaths(annotation));
            }
        }

        return null;
    }

    private List<String> extractPaths(PsiAnnotation annotation) {
        PsiAnnotationMemberValue valueAttr = annotation.findDeclaredAttributeValue("value");
        PsiAnnotationMemberValue pathAttr = annotation.findDeclaredAttributeValue("path");

        PsiAnnotationMemberValue target = valueAttr != null ? valueAttr : pathAttr;
        if (target == null) {
            return Collections.emptyList();
        }

        return extractStringValues(target);
    }

    private List<String> extractStringValues(PsiAnnotationMemberValue value) {
        if (value == null) {
            return Collections.emptyList();
        }

        if (value instanceof PsiArrayInitializerMemberValue arrayValue) {
            List<String> result = new ArrayList<>();
            for (PsiAnnotationMemberValue initializer : arrayValue.getInitializers()) {
                String parsed = parseStringLiteral(initializer);
                if (parsed != null) {
                    result.add(parsed);
                }
            }
            return result;
        }

        String parsed = parseStringLiteral(value);
        if (parsed == null) {
            return Collections.emptyList();
        }

        return List.of(parsed);
    }

    private String parseStringLiteral(PsiAnnotationMemberValue value) {
        if (value == null) {
            return null;
        }

        String text = value.getText();
        if (text == null) {
            return null;
        }

        text = text.trim();

        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            return text.substring(1, text.length() - 1);
        }

        if ("\"\"".equals(text)) {
            return "";
        }

        return text;
    }

    private String extractRequestMethod(PsiAnnotation annotation) {
        PsiAnnotationMemberValue methodAttr = annotation.findDeclaredAttributeValue("method");
        if (methodAttr == null) {
            return "REQUEST";
        }

        String text = methodAttr.getText();
        if (text == null || text.isBlank()) {
            return "REQUEST";
        }

        if (text.contains("RequestMethod.GET")) {
            return "GET";
        }
        if (text.contains("RequestMethod.POST")) {
            return "POST";
        }
        if (text.contains("RequestMethod.PUT")) {
            return "PUT";
        }
        if (text.contains("RequestMethod.DELETE")) {
            return "DELETE";
        }
        if (text.contains("RequestMethod.PATCH")) {
            return "PATCH";
        }

        return "REQUEST";
    }

    private String normalizePath(String classPath, String methodPath) {
        String base = sanitizePathPart(classPath);
        String sub = sanitizePathPart(methodPath);

        if (base.isEmpty() && sub.isEmpty()) {
            return "/";
        }

        if (base.isEmpty()) {
            return ensureStartsWithSlash(sub);
        }

        if (sub.isEmpty()) {
            return ensureStartsWithSlash(base);
        }

        String joined = trimTrailingSlash(base) + "/" + trimLeadingSlash(sub);
        return ensureStartsWithSlash(joined);
    }

    private String sanitizePathPart(String path) {
        if (path == null) {
            return "";
        }

        String trimmed = path.trim();
        if (trimmed.equals("/")) {
            return "";
        }
        return trimmed;
    }

    private String ensureStartsWithSlash(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }

        return path.startsWith("/") ? path : "/" + path;
    }

    private String trimLeadingSlash(String path) {
        if (path == null) {
            return "";
        }

        int index = 0;
        while (index < path.length() && path.charAt(index) == '/') {
            index++;
        }
        return path.substring(index);
    }

    private String trimTrailingSlash(String path) {
        if (path == null) {
            return "";
        }

        int end = path.length();
        while (end > 0 && path.charAt(end - 1) == '/') {
            end--;
        }
        return path.substring(0, end);
    }

    private record EndpointMapping(
            String httpMethod,
            List<String> paths
    ) {
    }
}