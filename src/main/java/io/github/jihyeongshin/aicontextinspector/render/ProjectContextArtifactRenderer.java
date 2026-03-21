package io.github.jihyeongshin.aicontextinspector.render;

import io.github.jihyeongshin.aicontextinspector.model.ContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.InterpretedRepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.RepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.project.RepresentativeFlowMetadataEvaluator;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProjectContextArtifactRenderer {
    private static final DateTimeFormatter GENERATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final int TOP_PACKAGE_LIMIT = 20;
    private static final int TOP_ROLE_DISTRIBUTION_LIMIT = 12;
    private static final int TOP_AFFINITY_DISTRIBUTION_LIMIT = 8;
    private static final int TOP_TRAIT_DISTRIBUTION_LIMIT = 5;
    private static final int TOP_PACKAGE_DISTRIBUTION_LIMIT = 10;
    private static final int SUMMARY_LIMIT = 5;
    private static final int TOP_FLOW_PATTERN_LIMIT = 10;
    private static final int TOP_TRANSITION_SUMMARY_LIMIT = 3;
    private static final int TOP_TERMINAL_ROLE_LIMIT = 5;
    private static final int UNKNOWN_SAMPLE_LIMIT = 20;

    private final RepresentativeFlowMetadataEvaluator representativeFlowMetadataEvaluator =
            new RepresentativeFlowMetadataEvaluator();

    public String renderProjectStructure(ProjectContextSnapshot snapshot) {
        List<ContextSnapshot> files = snapshot.files();
        StringBuilder sb = new StringBuilder();
        Map<String, Long> roleCounts = countBy(files, file -> normalize(file.classRole()));
        Map<String, Long> affinityCounts = countBy(files, file -> normalize(file.architectureAffinity()));
        Map<String, Long> traitCounts = countTraits(files);
        Map<String, Long> packageCounts = countBy(files, file -> normalize(file.packageName()));
        int totalFiles = files.size();
        int entryPointFiles = countEntryPoints(files);
        long unknownRoleFiles = countByRole(files, "Unknown");
        long unknownAffinityFiles = countByAffinity(files, "Unknown");

        sb.append("# Project Structure").append("\n\n");
        appendSnapshotSection(sb, List.of(
                entry("Generated at", formatGeneratedAt()),
                entry("Total Java files", String.valueOf(totalFiles)),
                entry("Endpoint files", String.valueOf(countEndpointFiles(files))),
                entry("Entry point files", String.valueOf(entryPointFiles)),
                entry("Unknown role files", String.valueOf(unknownRoleFiles)),
                entry("Unknown affinity files", String.valueOf(unknownAffinityFiles))
        ));

        appendBulletSection(sb, "Key Signals", List.of(
                "Entry point coverage: " + entryPointFiles + " of " + totalFiles + " files (" + formatPercentage(entryPointFiles, totalFiles) + ")",
                "Unknown role pressure: " + unknownRoleFiles + " of " + totalFiles + " files (" + formatPercentage(unknownRoleFiles, totalFiles) + ")",
                "Unknown affinity pressure: " + unknownAffinityFiles + " of " + totalFiles + " files (" + formatPercentage(unknownAffinityFiles, totalFiles) + ")",
                "Observed category breadth: roles " + roleCounts.size() + ", affinities " + affinityCounts.size() + ", traits " + traitCounts.size() + ", packages " + packageCounts.size(),
                "Dominant roles: " + formatTopSummary(roleCounts, 3),
                "Dominant affinities: " + formatTopSummary(affinityCounts, 3),
                "Dominant traits: " + formatTopSummary(traitCounts, 3),
                "Most populated packages: " + formatTopSummary(packageCounts, 5)
        ));

        appendUnknownSummary(sb, files);
        appendCompactCountSection(sb, "Role Distribution Highlights", roleCounts, TOP_ROLE_DISTRIBUTION_LIMIT, "Other roles");
        appendCompactCountSection(sb, "Affinity Distribution Highlights", affinityCounts, TOP_AFFINITY_DISTRIBUTION_LIMIT, "Other affinities");
        appendCompactCountSection(sb, "Trait Distribution", traitCounts, TOP_TRAIT_DISTRIBUTION_LIMIT, "Other traits");
        appendCompactCountSection(sb, "Package Distribution Highlights", packageCounts, TOP_PACKAGE_DISTRIBUTION_LIMIT, "Other packages");

        return sb.toString();
    }

    public String renderEntryPoints(ProjectContextSnapshot snapshot) {
        List<ContextSnapshot> entryPoints = snapshot.files().stream()
                .filter(this::isEntryPointLike)
                .sorted(Comparator
                        .comparing(ContextSnapshot::packageName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ContextSnapshot::className, Comparator.nullsLast(String::compareTo)))
                .toList();
        int totalEndpoints = entryPoints.stream()
                .mapToInt(file -> safeList(file.endpoints()).size())
                .sum();
        long entryPointPackages = entryPoints.stream()
                .map(file -> normalize(file.packageName()))
                .distinct()
                .count();
        long mappedEntryPoints = entryPoints.stream()
                .filter(file -> !safeList(file.endpoints()).isEmpty())
                .count();
        long unmappedEntryPoints = entryPoints.size() - mappedEntryPoints;
        Map<String, Long> entryPointPackageCounts = countBy(entryPoints, file -> normalize(file.packageName()));
        Map<String, Long> entryPointRoleCounts = countBy(entryPoints, file -> normalize(file.classRole()));
        double averageEndpointsPerEntryPoint = entryPoints.isEmpty()
                ? 0.0
                : totalEndpoints / (double) entryPoints.size();

        StringBuilder sb = new StringBuilder();
        sb.append("# Entry Points").append("\n\n");
        appendSnapshotSection(sb, List.of(
                entry("Generated at", formatGeneratedAt()),
                entry("Entry point files", String.valueOf(entryPoints.size())),
                entry("Controller role files", String.valueOf(countByRole(snapshot.files(), "Controller"))),
                entry("Detected endpoint routes", String.valueOf(totalEndpoints)),
                entry("Packages covered", String.valueOf(entryPointPackages)),
                entry("Mapped entry point ratio", mappedEntryPoints + " of " + entryPoints.size() + " files (" + formatPercentage(mappedEntryPoints, entryPoints.size()) + ")"),
                entry("Average endpoints per entry point", formatAverage(averageEndpointsPerEntryPoint))
        ));

        if (entryPoints.isEmpty()) {
            sb.append("No entry points were detected from the current project snapshot.").append("\n");
            return sb.toString();
        }

        appendBulletSection(sb, "Coverage", List.of(
                "Entry points with mapped endpoints: " + mappedEntryPoints,
                "Entry points without mapped endpoints: " + unmappedEntryPoints,
                "Entry point role profile: " + formatTopSummary(entryPointRoleCounts, 3),
                "Most active entry point packages: " + formatTopSummary(entryPointPackageCounts, SUMMARY_LIMIT)
        ));

        sb.append("## Entry Point Catalog").append("\n\n");
        for (ContextSnapshot entryPoint : entryPoints) {
            sb.append("### ").append(normalize(entryPoint.className())).append("\n");
            sb.append("- Package: ").append(normalize(entryPoint.packageName())).append("\n");
            sb.append("- Role: ").append(normalize(entryPoint.classRole())).append("\n");
            sb.append("- Affinity: ").append(normalize(entryPoint.architectureAffinity())).append("\n");
            sb.append("- Endpoint count: ").append(safeList(entryPoint.endpoints()).size()).append("\n");
            sb.append("- Dependencies: ").append(formatInlineList(entryPoint.dependencies())).append("\n");
            sb.append("#### Endpoints").append("\n");
            appendList(sb, safeList(entryPoint.endpoints()), "- ");
            sb.append("\n");
        }

        return sb.toString();
    }

    public String renderRepresentativeFlows(ProjectContextSnapshot snapshot, List<RepresentativeFlow> flows) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Representative Flows").append("\n\n");
        List<InterpretedRepresentativeFlow> interpretedFlows =
                representativeFlowMetadataEvaluator.evaluate(snapshot, flows);
        Map<String, Long> topPatterns = flows.stream()
                .collect(Collectors.groupingBy(
                        RepresentativeFlow::toRoleDisplayString,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        Map<String, Long> terminalRoleCounts = countTerminalRoles(flows);
        Map<String, Long> transitionCounts = countRoleTransitions(flows);
        Map<String, Long> confidenceCounts = interpretedFlows.stream()
                .collect(Collectors.groupingBy(
                        flow -> flow.metadata().confidence().displayName(),
                        TreeMap::new,
                        Collectors.counting()
                ));
        Map<String, Long> ambiguityCounts = interpretedFlows.stream()
                .collect(Collectors.groupingBy(
                        flow -> flow.metadata().ambiguity().displayName(),
                        TreeMap::new,
                        Collectors.counting()
                ));
        String dominantPattern = topPatterns.isEmpty()
                ? "None"
                : sortByCountDescending(topPatterns, 1).get(0).getKey();
        String dominantTerminalRole = terminalRoleCounts.isEmpty()
                ? "None"
                : formatTopSummary(terminalRoleCounts, 1);
        String highestScoringFlow = flows.isEmpty()
                ? "None"
                : flows.stream()
                .max(Comparator.comparingInt(RepresentativeFlow::score))
                .map(RepresentativeFlow::toDisplayString)
                .orElse("None");
        int maxFlowLength = flows.stream()
                .mapToInt(flow -> flow.classNames().size())
                .max()
                .orElse(0);

        appendSnapshotSection(sb, List.of(
                entry("Generated at", formatGeneratedAt()),
                entry("Total representative flows", String.valueOf(flows.size())),
                entry("Entry point files considered", String.valueOf(countEntryPoints(snapshot.files()))),
                entry("Distinct role patterns", String.valueOf(topPatterns.size())),
                entry("Average flow length", formatAverageFlowLength(flows)),
                entry("Maximum flow length", String.valueOf(maxFlowLength))
        ));

        if (flows.isEmpty()) {
            sb.append("No representative flows were generated from the current project snapshot.").append("\n");
            return sb.toString();
        }

        appendBulletSection(sb, "Highlights", List.of(
                "Dominant role pattern: " + dominantPattern,
                "Highest scoring flow: " + highestScoringFlow,
                "Dominant terminal role: " + dominantTerminalRole,
                "Common role transitions: " + formatTopSummary(transitionCounts, TOP_TRANSITION_SUMMARY_LIMIT),
                "Confidence profile: " + formatTopSummary(confidenceCounts, 3),
                "Ambiguity profile: " + formatTopSummary(ambiguityCounts, 3)
        ));

        sb.append("## Pattern Distribution").append("\n");
        appendMapList(sb, sortByCountDescending(topPatterns, TOP_FLOW_PATTERN_LIMIT), "- ");
        sb.append("\n");

        appendCompactCountSection(sb, "Terminal Role Distribution", terminalRoleCounts, TOP_TERMINAL_ROLE_LIMIT, "Other terminal roles");

        sb.append("## Flow Catalog").append("\n\n");
        int index = 1;
        for (InterpretedRepresentativeFlow interpretedFlow : interpretedFlows) {
            RepresentativeFlow flow = interpretedFlow.flow();
            sb.append("### ").append(index++).append(". ").append(flow.toDisplayString()).append("\n");
            sb.append("- Roles: ").append(flow.toRoleDisplayString()).append("\n");
            sb.append("- Flow length: ").append(flow.classNames().size()).append("\n");
            sb.append("- Score: ").append(flow.score()).append("\n");
            sb.append("- Confidence: ").append(interpretedFlow.metadata().confidence().displayName()).append("\n");
            sb.append("- Ambiguity: ").append(interpretedFlow.metadata().ambiguity().displayName()).append("\n");
            sb.append("- Notes: ").append(interpretedFlow.metadata().notesDisplayString()).append("\n\n");
        }

        return sb.toString();
    }

    public String renderArchitectureRules(ProjectContextSnapshot snapshot, List<RepresentativeFlow> flows) {
        List<ContextSnapshot> files = snapshot.files();
        StringBuilder sb = new StringBuilder();
        sb.append("# Architecture Rules").append("\n\n");
        Map<String, Long> patterns = flows.stream()
                .collect(Collectors.groupingBy(
                        RepresentativeFlow::toRoleDisplayString,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        Map<String, Long> terminalRoleCounts = countTerminalRoles(flows);
        String dominantPatternSummary = formatTopSummary(patterns, 1);
        String dominantTerminalRoleSummary = formatTopSummary(terminalRoleCounts, 1);

        appendSnapshotSection(sb, List.of(
                entry("Generated at", formatGeneratedAt()),
                entry("Representative flows referenced", String.valueOf(flows.size())),
                entry("Unknown affinity count", String.valueOf(countByAffinity(files, "Unknown"))),
                entry("Entry point layer profile", formatTopRoleSummary(files, "EntryPointLike")),
                entry("Application layer profile", formatTopRoleSummary(files, "ApplicationLike")),
                entry("Persistence layer profile", formatTopRoleSummary(files, "PersistenceLike")),
                entry("Adapter layer profile", formatTopRoleSummary(files, "AdapterLike")),
                entry("Dominant terminal role", dominantTerminalRoleSummary)
        ));

        appendBulletSection(sb, "Rule Summary", List.of(
                "Entry point layer is primarily represented by " + formatTopRoleSummary(files, "EntryPointLike") + ".",
                "Application layer is primarily represented by " + formatTopRoleSummary(files, "ApplicationLike") + ".",
                "Persistence layer is primarily represented by " + formatTopRoleSummary(files, "PersistenceLike") + ".",
                "Dominant representative pattern: " + dominantPatternSummary + "."
        ));

        appendBulletSection(sb, "Interpretation Profile", List.of(
                "Representative pattern diversity: " + patterns.size() + " distinct patterns across " + flows.size() + " flows.",
                "Dominant representative pattern: " + dominantPatternSummary + ".",
                "Dominant terminal role: " + dominantTerminalRoleSummary + "."
        ));

        sb.append("## Representative Flow Policy").append("\n");
        sb.append("- Main representative flows prefer EntryPointLike to ApplicationLike transitions.").append("\n");
        sb.append("- Main representative flows terminate at PersistenceLike or AdapterLike responsibilities.").append("\n");
        sb.append("- SupportLike, DataLike, DomainLike, TestLike, and EntryPointLike classes are excluded from mid-flow progression.").append("\n");
        sb.append("- Unknown affinity classes only participate when their role matches the architecture chain.").append("\n\n");

        if (flows.isEmpty()) {
            sb.append("## Representative Pattern Signals").append("\n");
            sb.append("- None").append("\n\n");
            sb.append("## Current Interpretation Notes").append("\n");
            sb.append("- Unknown role count remains acceptable when affinity or traits already explain the structure.").append("\n");
            sb.append("- Representative flows are architecture summaries, not raw runtime call traces.").append("\n");
            sb.append("- Project-specific edge cases should remain interpreted through flow output, not hardcoded as global rules.").append("\n");
            return sb.toString();
        }

        sb.append("## Representative Pattern Signals").append("\n");
        appendMapList(sb, sortByCountDescending(patterns, TOP_FLOW_PATTERN_LIMIT), "- ");
        sb.append("\n");

        sb.append("## Current Interpretation Notes").append("\n");
        sb.append("- Unknown role count remains acceptable when affinity or traits already explain the structure.").append("\n");
        sb.append("- Representative flows are architecture summaries, not raw runtime call traces.").append("\n");
        sb.append("- Project-specific edge cases should remain interpreted through flow output, not hardcoded as global rules.").append("\n");

        return sb.toString();
    }

    private void appendSnapshotSection(StringBuilder sb, List<Map.Entry<String, String>> entries) {
        sb.append("## Snapshot").append("\n");
        appendKeyValueEntries(sb, entries);
        sb.append("\n");
    }

    private void appendBulletSection(StringBuilder sb, String title, List<String> bullets) {
        sb.append("## ").append(title).append("\n");
        for (String bullet : bullets) {
            sb.append("- ").append(bullet).append("\n");
        }
        sb.append("\n");
    }

    private void appendKeyValueEntries(StringBuilder sb, List<Map.Entry<String, String>> entries) {
        for (Map.Entry<String, String> entry : entries) {
            sb.append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        }
    }

    private String formatGeneratedAt() {
        return ZonedDateTime.now().format(GENERATED_AT_FORMAT);
    }

    private Map.Entry<String, String> entry(String key, String value) {
        return Map.entry(key, value);
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

    private void appendCompactCountSection(
            StringBuilder sb,
            String title,
            Map<String, Long> counts,
            int limit,
            String remainderLabel
    ) {
        sb.append("## ").append(title).append("\n");

        List<Map.Entry<String, Long>> sortedEntries = sortByCountDescending(counts, counts.size());
        long total = counts.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        int visibleCount = Math.min(limit, sortedEntries.size());

        for (int index = 0; index < visibleCount; index++) {
            Map.Entry<String, Long> entry = sortedEntries.get(index);
            sb.append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append(" (")
                    .append(formatPercentage(entry.getValue(), total))
                    .append(")")
                    .append("\n");
        }

        if (sortedEntries.size() > visibleCount) {
            long remainingCount = sortedEntries.stream()
                    .skip(visibleCount)
                    .mapToLong(Map.Entry::getValue)
                    .sum();
            int remainingCategories = sortedEntries.size() - visibleCount;

            sb.append("- ")
                    .append(remainderLabel)
                    .append(": ")
                    .append(remainingCategories)
                    .append(" categories / ")
                    .append(remainingCount)
                    .append(" items")
                    .append(" (")
                    .append(formatPercentage(remainingCount, total))
                    .append(")")
                    .append("\n");
        }

        sb.append("\n");
    }

    private void appendUnknownSummary(StringBuilder sb, List<ContextSnapshot> files) {
        sb.append("## Unknown Samples").append("\n");
        appendUnknownList(
                sb,
                "Unclassified Roles",
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
                "Unclassified Affinity",
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

    private String formatTopSummary(Map<String, Long> counts, int limit) {
        List<Map.Entry<String, Long>> entries = sortByCountDescending(counts, limit);
        if (entries.isEmpty()) {
            return "None";
        }

        return entries.stream()
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .collect(Collectors.joining(", "));
    }

    private String formatAverageFlowLength(List<RepresentativeFlow> flows) {
        if (flows.isEmpty()) {
            return "0.00";
        }

        double average = flows.stream()
                .mapToInt(flow -> flow.classNames().size())
                .average()
                .orElse(0.0);
        return formatAverage(average);
    }

    private String formatAverage(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatPercentage(long value, long total) {
        if (total <= 0) {
            return "0.0%";
        }

        double percentage = (value * 100.0) / total;
        return String.format(Locale.ROOT, "%.1f%%", percentage);
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

    private Map<String, Long> countTerminalRoles(List<RepresentativeFlow> flows) {
        Map<String, Long> counts = new TreeMap<>();
        for (RepresentativeFlow flow : flows) {
            List<String> roles = safeList(flow.classRoles());
            if (roles.isEmpty()) {
                continue;
            }

            String terminalRole = normalize(roles.get(roles.size() - 1));
            counts.put(terminalRole, counts.getOrDefault(terminalRole, 0L) + 1);
        }
        return counts;
    }

    private Map<String, Long> countRoleTransitions(List<RepresentativeFlow> flows) {
        Map<String, Long> counts = new TreeMap<>();
        for (RepresentativeFlow flow : flows) {
            List<String> roles = safeList(flow.classRoles());
            for (int index = 0; index < roles.size() - 1; index++) {
                String transition = normalize(roles.get(index)) + " -> " + normalize(roles.get(index + 1));
                counts.put(transition, counts.getOrDefault(transition, 0L) + 1);
            }
        }
        return counts;
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
