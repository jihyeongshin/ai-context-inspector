package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodExtractor {

    public List<String> extract(PsiClass psiClass) {
        if (psiClass == null) {
            return Collections.emptyList();
        }

        List<String> methods = new ArrayList<>();

        for (PsiMethod method : psiClass.getMethods()) {
            if (method.isConstructor()) {
                continue;
            }

            methods.add(formatMethodSignature(method));
        }

        return methods;
    }

    private String formatMethodSignature(PsiMethod method) {
        String methodName = method.getName();
        String parameters = formatParameters(method.getParameterList());
        String returnType = formatReturnType(method);

        return methodName + "(" + parameters + "): " + returnType;
    }

    private String formatParameters(PsiParameterList parameterList) {
        PsiParameter[] parameters = parameterList.getParameters();
        if (parameters.length == 0) {
            return "";
        }

        List<String> formatted = new ArrayList<>();
        for (PsiParameter parameter : parameters) {
            formatted.add(formatParameter(parameter));
        }

        return String.join(", ", formatted);
    }

    private String formatParameter(PsiParameter parameter) {
        String typeText = extractTypeText(parameter.getTypeElement(), parameter.getType());
        String name = parameter.getName();

        if (name.isBlank()) {
            return typeText;
        }

        return typeText + " " + name;
    }

    private String formatReturnType(PsiMethod method) {
        PsiType returnType = method.getReturnType();
        if (returnType == null) {
            return "void";
        }

        return extractTypeText(method.getReturnTypeElement(), returnType);
    }

    private String extractTypeText(PsiTypeElement typeElement, PsiType fallbackType) {
        if (typeElement != null) {
            String text = typeElement.getText();
            if (text != null && !text.isBlank()) {
                return text.trim();
            }
        }

        if (fallbackType != null) {
            String presentableText = fallbackType.getPresentableText();
            if (!presentableText.isBlank()) {
                return presentableText.trim();
            }
        }

        return "Unknown";
    }
}