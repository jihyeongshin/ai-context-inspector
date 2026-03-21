package io.github.jihyeongshin.aicontextinspector.project;

import io.github.jihyeongshin.aicontextinspector.model.ProjectRule;
import io.github.jihyeongshin.aicontextinspector.model.ProjectRuleKind;
import io.github.jihyeongshin.aicontextinspector.model.ProjectRuleSet;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ProjectRuleValidator {
    private static final int SUPPORTED_VERSION = 1;

    public List<String> validate(ProjectRuleSet ruleSet) {
        List<String> warnings = new ArrayList<>();
        if (ruleSet == null) {
            warnings.add("Rule set is missing.");
            return warnings;
        }

        if (ruleSet.version() != SUPPORTED_VERSION) {
            warnings.add("Unsupported rule file version: " + ruleSet.version() + ". Only version 1 is supported.");
        }

        Set<String> seenIds = new LinkedHashSet<>();
        int index = 1;
        for (ProjectRule rule : ruleSet.rules()) {
            warnings.addAll(validateRule(rule, index++, seenIds));
        }

        return warnings;
    }

    public List<String> validateRule(ProjectRule rule, int index, Set<String> seenIds) {
        List<String> warnings = new ArrayList<>();
        if (rule == null) {
            warnings.add("Rule #" + index + " is missing.");
            return warnings;
        }

        if ("Unknown".equals(rule.id())) {
            warnings.add("Rule #" + index + " is missing id.");
        } else if (!seenIds.add(rule.id())) {
            warnings.add("Duplicate rule id: " + rule.id() + ".");
        }

        if (rule.kind() == null) {
            warnings.add("Rule " + rule.id() + " has unsupported or missing kind.");
            return warnings;
        }

        if (rule.description().isBlank()) {
            warnings.add("Rule " + rule.id() + " is missing description.");
        }

        if (rule.kind() == ProjectRuleKind.EXPECTED_TRANSITION) {
            if (rule.fromAffinity() == null) {
                warnings.add("Rule " + rule.id() + " is missing from_affinity.");
            }
            if (rule.toAffinityAnyOf().isEmpty()) {
                warnings.add("Rule " + rule.id() + " is missing to_affinity_any_of.");
            }
        }

        if ((rule.kind() == ProjectRuleKind.PREFERRED_TERMINAL_AFFINITY
                || rule.kind() == ProjectRuleKind.DISCOURAGED_MID_AFFINITY)
                && rule.affinityAnyOf().isEmpty()) {
            warnings.add("Rule " + rule.id() + " is missing affinity_any_of.");
        }

        return warnings;
    }
}
