package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;

import java.util.ArrayList;
import java.util.List;

public class MethodExtractor {

    public List<String> extract(PsiClass psiClass) {
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

}
