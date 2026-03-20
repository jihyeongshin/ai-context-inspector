package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;

// TODO: cache map, role index, domain grouping
public record ProjectContextSnapshot(
        List<ContextSnapshot> files
) {
    public ProjectContextSnapshot {
        files = files == null ? List.of() : List.copyOf(files);
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    public int size() {
        return files.size();
    }
}