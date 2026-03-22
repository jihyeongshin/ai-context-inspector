package io.github.jihyeongshin.aicontextinspector.analysis.rule;

import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRule;
import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRuleKind;
import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRuleLevel;
import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRuleLoadResult;
import io.github.jihyeongshin.aicontextinspector.model.rule.ProjectRuleSet;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectRuleLoader {
    public static final String DEFAULT_RULE_PATH = ".aiassistant/rules/project-rules.yaml";

    private final ProjectRuleValidator validator = new ProjectRuleValidator();

    public ProjectRuleLoadResult load(Path projectBasePath) {
        Path rulePath = projectBasePath == null
                ? Path.of(DEFAULT_RULE_PATH)
                : projectBasePath.resolve(DEFAULT_RULE_PATH);
        String normalizedPath = rulePath.normalize().toString();

        if (projectBasePath == null || !Files.exists(rulePath)) {
            return new ProjectRuleLoadResult(false, normalizedPath, ProjectRuleSet.empty(), List.of());
        }

        List<String> warnings = new ArrayList<>();
        try {
            String content = Files.readString(rulePath, StandardCharsets.UTF_8);
            Object loaded = new Yaml().load(content);
            if (!(loaded instanceof Map<?, ?> rawRoot)) {
                warnings.add("Rule file is malformed: top-level YAML object must be a map.");
                return new ProjectRuleLoadResult(true, normalizedPath, ProjectRuleSet.empty(), warnings);
            }

            ProjectRuleSet ruleSet = parseRuleSet(rawRoot, warnings);
            warnings.addAll(validator.validate(ruleSet));
            return new ProjectRuleLoadResult(
                    true,
                    normalizedPath,
                    ruleSet,
                    warnings
            );
        } catch (IOException exception) {
            warnings.add("Failed to read rule file: " + exception.getMessage());
        } catch (RuntimeException exception) {
            warnings.add("Rule file is malformed: " + exception.getMessage());
        }

        return new ProjectRuleLoadResult(true, normalizedPath, ProjectRuleSet.empty(), warnings);
    }

    private ProjectRuleSet parseRuleSet(Map<?, ?> rawRoot, List<String> warnings) {
        int version = parseVersion(rawRoot.get("version"), warnings);
        Object rawRules = rawRoot.get("rules");
        if (rawRules == null) {
            return new ProjectRuleSet(version, List.of());
        }
        if (!(rawRules instanceof List<?> rawRuleList)) {
            warnings.add("Rule file is malformed: rules must be a list.");
            return new ProjectRuleSet(version, List.of());
        }

        List<ProjectRule> rules = new ArrayList<>();
        int index = 1;
        for (Object rawRule : rawRuleList) {
            if (!(rawRule instanceof Map<?, ?> rawRuleMap)) {
                warnings.add("Rule #" + index + " is malformed: each rule must be a map.");
                index++;
                continue;
            }

            ProjectRule parsedRule = parseRule(rawRuleMap, index, warnings);
            if (parsedRule != null) {
                rules.add(parsedRule);
            }
            index++;
        }
        return new ProjectRuleSet(version, rules);
    }

    private int parseVersion(Object rawVersion, List<String> warnings) {
        if (rawVersion instanceof Number number) {
            return number.intValue();
        }
        if (rawVersion instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                warnings.add("Rule file version is malformed: " + text + ".");
                return 1;
            }
        }
        return 1;
    }

    private ProjectRule parseRule(Map<?, ?> rawRule, int index, List<String> warnings) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawRule.entrySet()) {
            normalized.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        String id = asString(normalized.get("id"));
        ProjectRuleKind kind = ProjectRuleKind.fromYamlValue(asString(normalized.get("kind")));
        ProjectRuleLevel level = ProjectRuleLevel.fromYamlValue(asString(normalized.get("level")));
        if (level == null) {
            level = ProjectRuleLevel.GUIDE;
        }

        if (kind == null) {
            warnings.add("Rule " + (id == null ? "#" + index : id) + " has unsupported or missing kind.");
            return null;
        }

        return new ProjectRule(
                id,
                kind,
                asString(normalized.get("description")),
                asString(normalized.get("from_affinity")),
                asStringList(normalized.get("to_affinity_any_of")),
                asStringList(normalized.get("affinity_any_of")),
                level
        );
    }

    private List<String> asStringList(Object value) {
        if (value instanceof List<?> rawList) {
            List<String> values = new ArrayList<>();
            for (Object rawValue : rawList) {
                String stringValue = asString(rawValue);
                if (stringValue != null && !stringValue.isBlank()) {
                    values.add(stringValue);
                }
            }
            return values;
        }
        return List.of();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}

