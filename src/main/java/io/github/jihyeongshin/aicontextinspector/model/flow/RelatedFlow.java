package io.github.jihyeongshin.aicontextinspector.model.flow;

import java.util.List;

public record RelatedFlow(
        List<String> classNames,
        int score
) {
    public String toDisplayString() {
        return String.join(" -> ", classNames);
    }
}
