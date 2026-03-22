package io.github.jihyeongshin.aicontextinspector.model.snapshot;

import java.nio.file.Path;
import java.util.List;

public record ProjectContextExportResult(
        Path outputDirectory,
        List<Path> files
) {
    public ProjectContextExportResult {
        files = files == null ? List.of() : List.copyOf(files);
    }
}
