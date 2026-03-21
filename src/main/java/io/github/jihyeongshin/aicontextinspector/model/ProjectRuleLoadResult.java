package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record ProjectRuleLoadResult(
        boolean ruleFileDetected,
        String ruleSourcePath,
        ProjectRuleSet ruleSet,
        List<String> warnings
) {
    public ProjectRuleLoadResult {
        ruleSourcePath = ruleSourcePath == null ? "Unknown" : ruleSourcePath;
        ruleSet = ruleSet == null ? ProjectRuleSet.empty() : ruleSet;
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public int rulesLoadedCount() {
        return ruleSet.rules().size();
    }

    public List<String> supportedRuleKindsSummary() {
        Map<String, Long> counts = ruleSet.rules().stream()
                .collect(Collectors.groupingBy(
                        rule -> rule.kind().yamlValue(),
                        TreeMap::new,
                        Collectors.counting()
                ));

        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .toList();
    }
}
