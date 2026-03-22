package io.github.jihyeongshin.aicontextinspector.model.snapshot;

import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRuleSet;
import io.github.jihyeongshin.aicontextinspector.model.source.ContextSnapshot;

import java.util.List;

// TODO: cache map, role index, domain grouping
public record ProjectContextSnapshot(
        List<ContextSnapshot> files,
        ProjectRuleSet projectRuleSet,
        boolean ruleFileDetected,
        String ruleSourcePath,
        int rulesLoadedCount,
        List<String> supportedRuleKindsSummary,
        List<String> ruleLoadWarnings
) {
    public ProjectContextSnapshot(List<ContextSnapshot> files) {
        this(files, ProjectRuleSet.empty(), false, "Unknown", 0, List.of(), List.of());
    }

    public ProjectContextSnapshot {
        files = files == null ? List.of() : List.copyOf(files);
        projectRuleSet = projectRuleSet == null ? ProjectRuleSet.empty() : projectRuleSet;
        ruleSourcePath = ruleSourcePath == null || ruleSourcePath.isBlank() ? "Unknown" : ruleSourcePath;
        rulesLoadedCount = Math.max(rulesLoadedCount, 0);
        supportedRuleKindsSummary = supportedRuleKindsSummary == null
                ? List.of()
                : List.copyOf(supportedRuleKindsSummary);
        ruleLoadWarnings = ruleLoadWarnings == null ? List.of() : List.copyOf(ruleLoadWarnings);
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    public int size() {
        return files.size();
    }

    public boolean hasProjectRules() {
        return rulesLoadedCount > 0;
    }
}
