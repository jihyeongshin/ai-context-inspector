package io.github.jihyeongshin.aicontextinspector.analysis.guidance;

import io.github.jihyeongshin.aicontextinspector.model.guidance.GuidanceSignal;
import io.github.jihyeongshin.aicontextinspector.model.guidance.GuidanceSignalStatus;
import io.github.jihyeongshin.aicontextinspector.model.guidance.ParserVisibilitySummary;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.source.ContextSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParserVisibilityEvaluator {
    private static final int SAMPLE_LIMIT = 3;
    private static final List<String> REQUEST_LIKE_TYPE_SUFFIXES = List.of(
            "Command",
            "Criteria",
            "Filter",
            "Input",
            "Query",
            "Request"
    );

    public ParserVisibilitySummary evaluate(ProjectContextSnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return new ParserVisibilitySummary(List.of(), List.of(), Map.of());
        }

        ParserVisibilityEvidence evidence = buildEvidence(snapshot.files());
        if (evidence.entryPointHelpers().isEmpty()) {
            return new ParserVisibilitySummary(List.of(), List.of(), Map.of());
        }

        List<GuidanceSignal> signals = new ArrayList<>();
        signals.add(new GuidanceSignal(
                "Parser visibility",
                GuidanceSignalStatus.PROVISIONAL,
                "Controller-adjacent request interpretation helpers are visible near "
                        + formatCount(evidence.entryPointHelpers().size(), "entry point")
                        + ". Sample helpers include "
                        + joinWithAnd(evidence.helperSamples())
                        + "."
        ));

        if (!evidence.typedFacadeSamples().isEmpty()) {
            signals.add(new GuidanceSignal(
                    "Facade handoff context",
                    GuidanceSignalStatus.PROVISIONAL,
                    "Some helper-bearing entry points also depend on Facades such as "
                            + joinWithAnd(evidence.typedFacadeSamples())
                            + " with request-like inputs, which supports reading these helpers as request-interpretation roles rather than generic utility noise."
            ));
        }

        List<String> notes = List.of(
                "These helpers can indicate raw request interpretation staying near the controller boundary.",
                "Their absence does not imply a problem; some projects rely on typed controller binding instead."
        );

        return new ParserVisibilitySummary(signals, notes, evidence.entryPointHelpers());
    }

    private ParserVisibilityEvidence buildEvidence(List<ContextSnapshot> files) {
        Map<String, List<String>> entryPointHelpers = new LinkedHashMap<>();
        Set<String> helperSamples = new LinkedHashSet<>();
        Set<String> typedFacadeSamples = new LinkedHashSet<>();
        Map<String, List<ContextSnapshot>> facadesByName = indexFacadesByClassName(files);

        for (ContextSnapshot file : safeList(files)) {
            if (!isEntryPoint(file)) {
                continue;
            }

            List<String> helpers = parserOrExtractorDependencies(file);
            if (helpers.isEmpty()) {
                continue;
            }

            entryPointHelpers.put(entryPointIdentity(file), helpers);
            helperSamples.addAll(helpers);

            for (String dependency : safeList(file.dependencies())) {
                for (ContextSnapshot facade : facadesByName.getOrDefault(normalize(dependency), List.of())) {
                    if (hasTypedRequestLikeMethod(facade)) {
                        typedFacadeSamples.add(normalize(facade.className()));
                    }
                }
            }
        }

        return new ParserVisibilityEvidence(
                Map.copyOf(entryPointHelpers),
                limitSamples(helperSamples),
                limitSamples(typedFacadeSamples)
        );
    }

    private Map<String, List<ContextSnapshot>> indexFacadesByClassName(List<ContextSnapshot> files) {
        Map<String, List<ContextSnapshot>> facades = new LinkedHashMap<>();
        for (ContextSnapshot file : safeList(files)) {
            if (!"Facade".equals(normalize(file.classRole()))) {
                continue;
            }
            facades.computeIfAbsent(normalize(file.className()), key -> new ArrayList<>()).add(file);
        }
        return facades;
    }

    private List<String> parserOrExtractorDependencies(ContextSnapshot file) {
        List<String> helpers = new ArrayList<>();
        for (String dependency : safeList(file.dependencies())) {
            String normalizedDependency = normalize(dependency);
            if (normalizedDependency.endsWith("Parser") || normalizedDependency.endsWith("Extractor")) {
                helpers.add(normalizedDependency);
            }
        }
        return helpers.stream()
                .distinct()
                .toList();
    }

    private boolean hasTypedRequestLikeMethod(ContextSnapshot file) {
        for (String method : safeList(file.methods())) {
            for (MethodParameter parameter : parseParameters(method)) {
                if (isTypedRequestLikeParameter(parameter)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<MethodParameter> parseParameters(String methodSignature) {
        if (methodSignature == null || methodSignature.isBlank()) {
            return List.of();
        }

        int start = methodSignature.indexOf('(');
        int end = methodSignature.lastIndexOf(')');
        if (start < 0 || end <= start) {
            return List.of();
        }

        String parameterBlock = methodSignature.substring(start + 1, end).trim();
        if (parameterBlock.isBlank()) {
            return List.of();
        }

        List<MethodParameter> parameters = new ArrayList<>();
        for (String token : splitParameters(parameterBlock)) {
            String trimmed = token.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            int lastSpace = trimmed.lastIndexOf(' ');
            if (lastSpace < 0) {
                parameters.add(new MethodParameter(trimmed, ""));
                continue;
            }

            parameters.add(new MethodParameter(
                    trimmed.substring(0, lastSpace).trim(),
                    trimmed.substring(lastSpace + 1).trim()
            ));
        }
        return parameters;
    }

    private List<String> splitParameters(String parameterBlock) {
        List<String> parameters = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int genericDepth = 0;

        for (int index = 0; index < parameterBlock.length(); index++) {
            char currentChar = parameterBlock.charAt(index);
            if (currentChar == '<') {
                genericDepth++;
            } else if (currentChar == '>') {
                genericDepth = Math.max(0, genericDepth - 1);
            } else if (currentChar == ',' && genericDepth == 0) {
                parameters.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(currentChar);
        }

        if (current.length() > 0) {
            parameters.add(current.toString());
        }

        return parameters;
    }

    private boolean isTypedRequestLikeParameter(MethodParameter parameter) {
        String typeName = simpleTypeName(parameter.type());
        for (String suffix : REQUEST_LIKE_TYPE_SUFFIXES) {
            if (typeName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private String simpleTypeName(String type) {
        String normalizedType = normalize(type);
        int genericStart = normalizedType.indexOf('<');
        if (genericStart >= 0) {
            normalizedType = normalizedType.substring(0, genericStart).trim();
        }
        int arrayStart = normalizedType.indexOf('[');
        if (arrayStart >= 0) {
            normalizedType = normalizedType.substring(0, arrayStart).trim();
        }
        normalizedType = normalizedType.replace("...", "").trim();
        int packageSeparator = normalizedType.lastIndexOf('.');
        if (packageSeparator >= 0) {
            normalizedType = normalizedType.substring(packageSeparator + 1).trim();
        }
        return normalizedType;
    }

    private List<String> limitSamples(Set<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(SAMPLE_LIMIT)
                .toList();
    }

    private boolean isEntryPoint(ContextSnapshot file) {
        return "Controller".equals(normalize(file.classRole()))
                || "EntryPointLike".equals(normalize(file.architectureAffinity()))
                || !safeList(file.endpoints()).isEmpty();
    }

    public String entryPointIdentity(ContextSnapshot file) {
        return normalize(file.packageName()) + "." + normalize(file.className());
    }

    private String joinWithAnd(List<String> values) {
        List<String> normalized = safeList(values).stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
        if (normalized.isEmpty()) {
            return "current extracted evidence";
        }
        if (normalized.size() == 1) {
            return normalized.get(0);
        }
        if (normalized.size() == 2) {
            return normalized.get(0) + " and " + normalized.get(1);
        }
        return String.join(", ", normalized.subList(0, normalized.size() - 1))
                + ", and "
                + normalized.get(normalized.size() - 1);
    }

    private String formatCount(long count, String singularNoun) {
        return count + " " + (count == 1 ? singularNoun : singularNoun + "s");
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value.trim();
    }

    private record ParserVisibilityEvidence(
            Map<String, List<String>> entryPointHelpers,
            List<String> helperSamples,
            List<String> typedFacadeSamples
    ) {
    }

    private record MethodParameter(String type, String name) {
    }
}
