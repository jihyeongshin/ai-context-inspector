package io.github.jihyeongshin.aicontextinspector.render;

import io.github.jihyeongshin.aicontextinspector.model.ContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.RepresentativeFlow;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProjectContextArtifactRenderer {
    private static final DateTimeFormatter GENERATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final int TOP_PACKAGE_LIMIT = 20;
    private static final int TOP_FLOW_PATTERN_LIMIT = 10;
    private static final int UNKNOWN_SAMPLE_LIMIT = 20;

    public String renderProjectStructure(ProjectContextSnapshot snapshot) {
        List<ContextSnapshot> files = snapshot.files();
        StringBuilder sb = new StringBuilder();

        sb.append("# Project Structure").append("\n\n");
        appendGeneratedAt(sb);

        sb.append("## Summary").append("\n");
        sb.append("- Total Java files: ").append(files.size()).append("\n");
        sb.append("- Endpoint files: ").append(countEndpointFiles(files)).append("\n");
        sb.append("- Unknown role files: ").append(countByRole(files, "Unknown")).append("\n");
        sb.append("- Unknown affinity files: ").append(countByAffinity(files, "Unknown")).append("\n\n");

        appendCountSection(sb, "Role Counts", countBy(files, file -> normalize(file.classRole())));
        appendCountSection(sb, "Affinity Counts", countBy(files, file -> normalize(file.architectureAffinity())));
        appendCountSection(sb, "Trait Counts", countTraits(files));
        appendCountSection(sb, "Top Packages", countBy(files, file -> normalize(file.packageName())), TOP_PACKAGE_LIMIT);
        appendUnknownSummary(sb, files);

        return sb.toString();
    }

    public String renderEntryPoints(ProjectContextSnapshot snapshot) {
        List<ContextSnapshot> entryPoints = snapshot.files().stream()
                .filter(this::isEntryPointLike)
                .sorted(Comparator
                        .comparing(ContextSnapshot::packageName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ContextSnapshot::className, Comparator.nullsLast(String::compareTo)))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("# Entry Points").append("\n\n");
        appendGeneratedAt(sb);

        sb.append("## Summary").append("\n");
        sb.append("- Entry point files: ").append(entryPoints.size()).append("\n");
        sb.append("- Controller role files: ").append(countByRole(snapshot.files(), "Controller")).append("\n\n");

        if (entryPoints.isEmpty()) {
            sb.append("No entry points were detected.").append("\n");
            return sb.toString();
        }

        sb.append("## Files").append("\n\n");
        for (ContextSnapshot entryPoint : entryPoints) {
            sb.append("### ").append(normalize(entryPoint.className())).append("\n");
            sb.append("- Package: ").append(normalize(entryPoint.packageName())).append("\n");
            sb.append("- Role: ").append(normalize(entryPoint.classRole())).append("\n");
            sb.append("- Affinity: ").append(normalize(entryPoint.architectureAffinity())).append("\n");
            sb.append("- Dependencies: ").append(formatInlineList(entryPoint.dependencies())).append("\n");
            sb.append("- Endpoints:").append("\n");
            appendList(sb, safeList(entryPoint.endpoints()), "  - ");
            sb.append("\n");
        }

        return sb.toString();
    }

    public String renderRepresentativeFlows(ProjectContextSnapshot snapshot, List<RepresentativeFlow> flows) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Representative Flows").append("\n\n");
        appendGeneratedAt(sb);

        sb.append("## Summary").append("\n");
        sb.append("- Total representative flows: ").append(flows.size()).append("\n");
        sb.append("- Entry point files considered: ").append(countEntryPoints(snapshot.files())).append("\n\n");

        if (flows.isEmpty()) {
            sb.append("No representative flows were generated.").append("\n");
            return sb.toString();
        }

        Map<String, Long> topPatterns = flows.stream()
                .collect(Collectors.groupingBy(
                        RepresentativeFlow::toRoleDisplayString,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        sb.append("## Top Flow Patterns").append("\n");
        appendMapList(sb, sortByCountDescending(topPatterns, TOP_FLOW_PATTERN_LIMIT), "- ");
        sb.append("\n");

        sb.append("## Flows").append("\n\n");
        int index = 1;
        for (RepresentativeFlow flow : flows) {
            sb.append("### ").append(index++).append(". ").append(flow.toDisplayString()).append("\n");
            sb.append("- Roles: ").append(flow.toRoleDisplayString()).append("\n");
            sb.append("- Score: ").append(flow.score()).append("\n\n");
        }

        return sb.toString();
    }

    public String renderArchitectureRules(ProjectContextSnapshot snapshot, List<RepresentativeFlow> flows) {
        List<ContextSnapshot> files = snapshot.files();
        StringBuilder sb = new StringBuilder();
        sb.append("# Architecture Rules").append("\n\n");
        appendGeneratedAt(sb);

        sb.append("## Observed Layer Signals").append("\n");
        sb.append("- Entry points: ").append(formatTopRoleSummary(files, "EntryPointLike")).append("\n");
        sb.append("- Application layer: ").append(formatTopRoleSummary(files, "ApplicationLike")).append("\n");
        sb.append("- Persistence layer: ").append(formatTopRoleSummary(files, "PersistenceLike")).append("\n");
        sb.append("- Adapter layer: ").append(formatTopRoleSummary(files, "AdapterLike")).append("\n");
        sb.append("- Unknown affinity count: ").append(countByAffinity(files, "Unknown")).append("\n\n");

        sb.append("## Representative Flow Policy").append("\n");
        sb.append("- Main representative flows prefer EntryPointLike to ApplicationLike transitions.").append("\n");
        sb.append("- Main representative flows terminate at PersistenceLike or AdapterLike responsibilities.").append("\n");
        sb.append("- SupportLike, DataLike, DomainLike, TestLike, and EntryPointLike classes are excluded from mid-flow progression.").append("\n");
        sb.append("- Unknown affinity classes only participate when their role matches the architecture chain.").append("\n\n");

        sb.append("## Observed Representative Patterns").append("\n");
        if (flows.isEmpty()) {
            sb.append("- None").append("\n");
            return sb.toString();
        }

        Map<String, Long> patterns = flows.stream()
                .collect(Collectors.groupingBy(
                        RepresentativeFlow::toRoleDisplayString,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        appendMapList(sb, sortByCountDescending(patterns, TOP_FLOW_PATTERN_LIMIT), "- ");
        sb.append("\n");

        sb.append("## Current Interpretation Notes").append("\n");
        sb.append("- Unknown role count remains acceptable when affinity or traits already explain the structure.").append("\n");
        sb.append("- Representative flows are architecture summaries, not raw runtime call traces.").append("\n");
        sb.append("- Project-specific edge cases should remain interpreted through flow output, not hardcoded as global rules.").append("\n");

        return sb.toString();
    }

    private void appendGeneratedAt(StringBuilder sb) {
        sb.append("- Generated at: ")
                .append(ZonedDateTime.now().format(GENERATED_AT_FORMAT))
                .append("\n\n");
    }

    private void appendCountSection(StringBuilder sb, String title, Map<String, Long> counts) {
        appendCountSection(sb, title, counts, counts.size());
    }

    private void appendCountSection(StringBuilder sb, String title, Map<String, Long> counts, int limit) {
        sb.append("## ").append(title).append("\n");
        List<Map.Entry<String, Long>> entries = sortByCountDescending(counts, limit);
        appendMapList(sb, entries, "- ");
        sb.append("\n");
    }

    private void appendUnknownSummary(StringBuilder sb, List<ContextSnapshot> files) {
        sb.append("## Unknown Summary").append("\n");
        appendUnknownList(
                sb,
                "Unknown Roles",
                files.stream()
                        .filter(file -> "Unknown".equals(normalize(file.classRole())))
                        .map(file -> normalize(file.className()))
                        .distinct()
                        .sorted()
                        .limit(UNKNOWN_SAMPLE_LIMIT)
                        .toList()
        );
        appendUnknownList(
                sb,
                "Unknown Affinities",
                files.stream()
                        .filter(file -> "Unknown".equals(normalize(file.architectureAffinity())))
                        .map(file -> normalize(file.className()))
                        .distinct()
                        .sorted()
                        .limit(UNKNOWN_SAMPLE_LIMIT)
                        .toList()
        );
    }

    private void appendUnknownList(StringBuilder sb, String title, List<String> values) {
        sb.append("### ").append(title).append("\n");
        appendList(sb, values, "- ");
        sb.append("\n");
    }

    private void appendList(StringBuilder sb, List<String> values, String prefix) {
        if (values.isEmpty()) {
            sb.append(prefix).append("None").append("\n");
            return;
        }

        for (String value : values) {
            sb.append(prefix).append(value).append("\n");
        }
    }

    private void appendMapList(StringBuilder sb, List<Map.Entry<String, Long>> entries, String prefix) {
        if (entries.isEmpty()) {
            sb.append(prefix).append("None").append("\n");
            return;
        }

        for (Map.Entry<String, Long> entry : entries) {
            sb.append(prefix)
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }
    }

    private Map<String, Long> countBy(List<ContextSnapshot> files, Function<ContextSnapshot, String> classifier) {
        return files.stream()
                .collect(Collectors.groupingBy(
                        classifier,
                        TreeMap::new,
                        Collectors.counting()
                ));
    }

    private Map<String, Long> countTraits(List<ContextSnapshot> files) {
        Map<String, Long> counts = new TreeMap<>();
        for (ContextSnapshot file : files) {
            for (String trait : safeList(file.structuralTraits())) {
                String normalized = normalize(trait);
                counts.put(normalized, counts.getOrDefault(normalized, 0L) + 1);
            }
        }
        return counts;
    }

    private int countEndpointFiles(List<ContextSnapshot> files) {
        return (int) files.stream()
                .filter(file -> !safeList(file.endpoints()).isEmpty())
                .count();
    }

    private int countEntryPoints(List<ContextSnapshot> files) {
        return (int) files.stream()
                .filter(this::isEntryPointLike)
                .count();
    }

    private long countByRole(List<ContextSnapshot> files, String role) {
        return files.stream()
                .filter(file -> role.equals(normalize(file.classRole())))
                .count();
    }

    private long countByAffinity(List<ContextSnapshot> files, String affinity) {
        return files.stream()
                .filter(file -> affinity.equals(normalize(file.architectureAffinity())))
                .count();
    }

    private boolean isEntryPointLike(ContextSnapshot file) {
        return "EntryPointLike".equals(normalize(file.architectureAffinity()))
                || "Controller".equals(normalize(file.classRole()))
                || !safeList(file.endpoints()).isEmpty();
    }

    private String formatTopRoleSummary(List<ContextSnapshot> files, String affinity) {
        Map<String, Long> roles = files.stream()
                .filter(file -> affinity.equals(normalize(file.architectureAffinity())))
                .collect(Collectors.groupingBy(
                        file -> normalize(file.classRole()),
                        TreeMap::new,
                        Collectors.counting()
                ));

        if (roles.isEmpty()) {
            return "None";
        }

        return sortByCountDescending(roles, 3).stream()
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
    }

    private String formatInlineList(List<String> values) {
        List<String> normalized = safeList(values).stream()
                .map(this::normalize)
                .toList();
        if (normalized.isEmpty()) {
            return "None";
        }
        return String.join(", ", normalized);
    }

    private List<Map.Entry<String, Long>> sortByCountDescending(Map<String, Long> counts, int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .toList();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }
}
