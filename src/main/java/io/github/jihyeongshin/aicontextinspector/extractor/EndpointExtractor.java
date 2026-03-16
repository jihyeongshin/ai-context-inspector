package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import java.util.ArrayList;
import java.util.List;

public class EndpointExtractor {

    public List<String> extract(PsiClass psiClass) {
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

}
