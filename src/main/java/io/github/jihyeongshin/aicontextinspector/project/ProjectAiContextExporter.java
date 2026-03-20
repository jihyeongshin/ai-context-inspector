package io.github.jihyeongshin.aicontextinspector.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.jihyeongshin.aicontextinspector.model.ProjectContextExportResult;
import io.github.jihyeongshin.aicontextinspector.model.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.RepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.render.ProjectContextArtifactRenderer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectAiContextExporter {
    private final RepresentativeFlowBuilder representativeFlowBuilder = new RepresentativeFlowBuilder();
    private final ProjectContextArtifactRenderer artifactRenderer = new ProjectContextArtifactRenderer();

    public ProjectContextExportResult export(Project project, ProjectContextSnapshot snapshot) throws IOException {
        if (project == null) {
            throw new IOException("Project is not available.");
        }

        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw new IOException("Project base path is not available.");
        }

        Path outputDirectory = Path.of(basePath, ".aiassistant", "context");
        Files.createDirectories(outputDirectory);

        List<RepresentativeFlow> representativeFlows = representativeFlowBuilder.build(snapshot);
        List<Path> writtenFiles = new ArrayList<>();

        writtenFiles.add(write(outputDirectory.resolve("project-structure.md"),
                artifactRenderer.renderProjectStructure(snapshot)));
        writtenFiles.add(write(outputDirectory.resolve("entrypoints.md"),
                artifactRenderer.renderEntryPoints(snapshot)));
        writtenFiles.add(write(outputDirectory.resolve("representative-flows.md"),
                artifactRenderer.renderRepresentativeFlows(snapshot, representativeFlows)));
        writtenFiles.add(write(outputDirectory.resolve("architecture-rules.md"),
                artifactRenderer.renderArchitectureRules(snapshot, representativeFlows)));

        refreshOutputDirectory(outputDirectory);

        return new ProjectContextExportResult(outputDirectory, writtenFiles);
    }

    private Path write(Path target, String content) throws IOException {
        Files.writeString(target, content, StandardCharsets.UTF_8);
        return target;
    }

    private void refreshOutputDirectory(Path outputDirectory) {
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(outputDirectory);
        if (virtualFile != null) {
            virtualFile.refresh(false, true);
        }
    }
}
