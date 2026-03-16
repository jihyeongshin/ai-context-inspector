package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;

import java.util.ArrayList;
import java.util.List;

public class FieldExtractor {

    public List<String> extract(PsiClass psiClass) {
        List<String> fields = new ArrayList<>();
        for (PsiField field : psiClass.getFields()) {
            String type = field.getType().getPresentableText();
            String name = field.getName();
            fields.add(type + " " + name);
        }
        return fields;
    }

}
