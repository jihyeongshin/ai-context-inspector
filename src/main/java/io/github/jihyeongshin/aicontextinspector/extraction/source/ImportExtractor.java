package io.github.jihyeongshin.aicontextinspector.extraction.source;

import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiJavaFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportExtractor {

    public List<String> extract(PsiJavaFile javaFile) {
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

}
