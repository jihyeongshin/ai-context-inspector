package io.github.jihyeongshin.aicontextinspector.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ContextPreviewDialog extends DialogWrapper {

    private final String message;

    public ContextPreviewDialog(Project project, String message) {
        super(project);
        this.message = message;

        setTitle("AI Context Inspector");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textArea.setLineWrap(false);
        textArea.setWrapStyleWord(false);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(900, 600));
        return scrollPane;
    }
}