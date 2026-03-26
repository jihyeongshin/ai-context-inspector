package io.github.jihyeongshin.aicontextinspector.analysis.guidance;

import io.github.jihyeongshin.aicontextinspector.model.guidance.GuidanceSignal;
import io.github.jihyeongshin.aicontextinspector.model.guidance.GuidanceSignalStatus;
import io.github.jihyeongshin.aicontextinspector.model.guidance.RawInputBoundaryGuidanceSummary;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.source.ContextSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RawInputBoundaryGuidanceEvaluator {
    private static final int SAMPLE_LIMIT = 3;
    private static final Set<String> RAW_STRING_PARAM_NAME_TOKENS = Set.of(
            "body",
            "content",
            "form",
            "json",
            "model",
            "payload",
            "raw",
            "request"
    );
    private static final Set<String> RAW_PARAM_TYPE_NAMES = Set.of(
            "HttpServletRequest",
            "HttpServletResponse",
            "JsonArray",
            "JsonElement",
            "JsonNode",
            "JsonObject",
            "JSONObject",
            "Map",
            "ModelMap",
            "MultiValueMap",
            "NativeWebRequest",
            "ObjectNode",
            "ServletRequest",
            "ServletResponse",
            "WebRequest"
    );
    private static final List<String> JSON_IMPORT_PREFIXES = List.of(
            "com.fasterxml.jackson.core",
            "com.fasterxml.jackson.databind",
            "com.fasterxml.jackson.dataformat",
            "com.google.gson",
            "jakarta.json",
            "javax.json",
            "net.minidev.json",
            "org.json"
    );
    private static final List<String> SERVLET_IMPORT_PREFIXES = List.of(
            "jakarta.servlet",
            "javax.servlet"
    );
    private static final List<String> REQUEST_LIKE_TYPE_SUFFIXES = List.of(
            "Command",
            "Criteria",
            "Filter",
            "Input",
            "Query",
            "Request"
    );

    public RawInputBoundaryGuidanceSummary evaluate(ProjectContextSnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return new RawInputBoundaryGuidanceSummary(
                    List.of(new GuidanceSignal(
                            "Boundary posture",
                            GuidanceSignalStatus.NOT_APPLICABLE,
                            "Current extracted evidence does not yet surface a raw-input boundary signal."
                    )),
                    List.of("These signals stay limited to current extracted evidence.")
            );
        }

        RawInputBoundaryEvidence evidence = buildEvidence(snapshot.files());
        return new RawInputBoundaryGuidanceSummary(
                List.of(
                        buildBoundaryPostureSignal(evidence),
                        buildParserBoundarySignal(evidence)
                ),
                buildNotes(evidence)
        );
    }

    private RawInputBoundaryEvidence buildEvidence(List<ContextSnapshot> files) {
        Set<String> rawParamUseCaseSamples = new LinkedHashSet<>();
        Set<String> useCaseParsingSamples = new LinkedHashSet<>();
        Set<String> controllerParserSamples = new LinkedHashSet<>();
        Set<String> typedFacadeSamples = new LinkedHashSet<>();

        int rawParamUseCaseCount = 0;
        int useCaseParsingCount = 0;
        int controllerParserCount = 0;
        int typedFacadeCount = 0;

        for (ContextSnapshot file : safeList(files)) {
            if (isUseCase(file) && hasRawInputLikeMethodParameter(file)) {
                rawParamUseCaseCount++;
                rawParamUseCaseSamples.add(normalize(file.className()));
            }

            if (isUseCase(file) && importsJsonOrServletApis(file)) {
                useCaseParsingCount++;
                useCaseParsingSamples.add(normalize(file.className()));
            }

            if (isEntryPoint(file)) {
                List<String> parserDependencies = parserOrExtractorDependencies(file);
                if (!parserDependencies.isEmpty()) {
                    controllerParserCount++;
                    controllerParserSamples.addAll(parserDependencies);
                }
            }

            if (isFacade(file) && hasTypedRequestLikeFacadeMethod(file)) {
                typedFacadeCount++;
                typedFacadeSamples.add(normalize(file.className()));
            }
        }

        return new RawInputBoundaryEvidence(
                rawParamUseCaseCount,
                limitSamples(rawParamUseCaseSamples),
                useCaseParsingCount,
                limitSamples(useCaseParsingSamples),
                controllerParserCount,
                limitSamples(controllerParserSamples),
                typedFacadeCount,
                limitSamples(typedFacadeSamples)
        );
    }

    private GuidanceSignal buildBoundaryPostureSignal(RawInputBoundaryEvidence evidence) {
        boolean hasLeakSignal = evidence.rawParamUseCaseCount() > 0 || evidence.useCaseParsingCount() > 0;
        boolean hasHealthyBoundarySignal = evidence.controllerParserCount() > 0 || evidence.typedFacadeCount() > 0;

        if (hasLeakSignal) {
            List<String> reasons = new ArrayList<>();
            if (evidence.rawParamUseCaseCount() > 0) {
                reasons.add(formatCountWithVerb(
                        evidence.rawParamUseCaseCount(),
                        "UseCase file",
                        "exposes",
                        "expose"
                ) + " raw-shaped method parameters");
            }
            if (evidence.useCaseParsingCount() > 0) {
                reasons.add(formatCountWithVerb(
                        evidence.useCaseParsingCount(),
                        "UseCase file",
                        "imports",
                        "import"
                ) + " JSON or Servlet APIs");
            }

            return new GuidanceSignal(
                    "Boundary posture",
                    GuidanceSignalStatus.CAUTION,
                    "Current extracted evidence suggests raw request interpretation may extend beyond the controller/parser boundary because "
                            + joinWithAnd(reasons)
                            + "."
            );
        }

        if (evidence.controllerParserCount() > 0 && evidence.typedFacadeCount() > 0) {
            return new GuidanceSignal(
                    "Boundary posture",
                    GuidanceSignalStatus.STRONG_ORIENTATION,
                    "Current extracted evidence suggests request interpretation is staying near the controller boundary because "
                            + formatCountWithVerb(
                            evidence.controllerParserCount(),
                            "entry point",
                            "depends",
                            "depend"
                    )
                            + " on parser or extractor collaborators and "
                            + formatCountWithVerb(
                            evidence.typedFacadeCount(),
                            "Facade file",
                            "exposes",
                            "expose"
                    )
                            + " typed request-like inputs before UseCase handoff."
            );
        }

        if (hasHealthyBoundarySignal) {
            return new GuidanceSignal(
                    "Boundary posture",
                    GuidanceSignalStatus.PROVISIONAL,
                    "Current extracted evidence suggests some request interpretation boundary is present near controllers or Facades, but the signal remains partial."
            );
        }

        return new GuidanceSignal(
                "Boundary posture",
                GuidanceSignalStatus.NOT_APPLICABLE,
                "Current extracted evidence does not surface a distinct raw-input boundary signal. Parser-free projects may still be healthy when typed binding already happens before the application boundary."
        );
    }

    private GuidanceSignal buildParserBoundarySignal(RawInputBoundaryEvidence evidence) {
        if (evidence.controllerParserCount() > 0) {
            String parserSamples = joinWithAnd(evidence.controllerParserSamples());
            String suffix = evidence.typedFacadeCount() > 0
                    ? " Facade samples such as " + joinWithAnd(evidence.typedFacadeSamples()) + " also suggest typed normalization before UseCase handoff."
                    : "";
            return new GuidanceSignal(
                    "Controller-adjacent interpretation role",
                    GuidanceSignalStatus.PROVISIONAL,
                    "Controller-adjacent parser or extractor dependencies such as "
                            + parserSamples
                            + " suggest a healthy request-interpretation role near entry points."
                            + suffix
            );
        }

        if (evidence.typedFacadeCount() > 0) {
            return new GuidanceSignal(
                    "Controller-adjacent interpretation role",
                    GuidanceSignalStatus.PROVISIONAL,
                    "Facade samples such as "
                            + joinWithAnd(evidence.typedFacadeSamples())
                            + " expose typed request-like inputs, which suggests normalization before UseCase handoff even without an explicit parser signal."
            );
        }

        return new GuidanceSignal(
                "Controller-adjacent interpretation role",
                GuidanceSignalStatus.NOT_APPLICABLE,
                "Current extracted evidence does not surface a distinct parser or extractor boundary. That can still be acceptable when typed controller binding already keeps raw input away from the application boundary."
        );
    }

    private List<String> buildNotes(RawInputBoundaryEvidence evidence) {
        List<String> notes = new ArrayList<>();
        notes.add("These are candidate guidance signals based on method signatures, imports, and entry point dependencies.");
        notes.add("Parser-free projects may still be healthy when raw input is already normalized before Facade or UseCase boundaries.");

        if (evidence.rawParamUseCaseCount() > 0) {
            notes.add("Raw-shaped UseCase parameter samples: " + joinWithAnd(evidence.rawParamUseCaseSamples()) + ".");
        } else if (evidence.useCaseParsingCount() > 0) {
            notes.add("UseCase JSON or Servlet import samples: " + joinWithAnd(evidence.useCaseParsingSamples()) + ".");
        }

        return notes.stream()
                .limit(3)
                .toList();
    }

    private boolean hasRawInputLikeMethodParameter(ContextSnapshot file) {
        for (String method : safeList(file.methods())) {
            for (MethodParameter parameter : parseParameters(method)) {
                if (isRawInputLikeParameter(parameter)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasTypedRequestLikeFacadeMethod(ContextSnapshot file) {
        for (String method : safeList(file.methods())) {
            for (MethodParameter parameter : parseParameters(method)) {
                if (isTypedRequestLikeParameter(parameter)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean importsJsonOrServletApis(ContextSnapshot file) {
        for (String importedType : safeList(file.imports())) {
            String normalizedImport = normalize(importedType);
            for (String prefix : JSON_IMPORT_PREFIXES) {
                if (normalizedImport.startsWith(prefix)) {
                    return true;
                }
            }
            for (String prefix : SERVLET_IMPORT_PREFIXES) {
                if (normalizedImport.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> parserOrExtractorDependencies(ContextSnapshot file) {
        List<String> parserDependencies = new ArrayList<>();
        for (String dependency : safeList(file.dependencies())) {
            String normalizedDependency = normalize(dependency);
            if (normalizedDependency.endsWith("Parser") || normalizedDependency.endsWith("Extractor")) {
                parserDependencies.add(normalizedDependency);
            }
        }
        return parserDependencies.stream()
                .distinct()
                .limit(SAMPLE_LIMIT)
                .toList();
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

    private boolean isRawInputLikeParameter(MethodParameter parameter) {
        String typeName = simpleTypeName(parameter.type());
        if (RAW_PARAM_TYPE_NAMES.contains(typeName)) {
            return true;
        }
        if (!"String".equals(typeName)) {
            return false;
        }

        String parameterName = normalize(parameter.name()).toLowerCase(Locale.ROOT);
        for (String token : RAW_STRING_PARAM_NAME_TOKENS) {
            if (parameterName.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTypedRequestLikeParameter(MethodParameter parameter) {
        if (isRawInputLikeParameter(parameter)) {
            return false;
        }

        String typeName = simpleTypeName(parameter.type());
        for (String suffix : REQUEST_LIKE_TYPE_SUFFIXES) {
            if (typeName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUseCase(ContextSnapshot file) {
        return "UseCase".equals(normalize(file.classRole()));
    }

    private boolean isFacade(ContextSnapshot file) {
        return "Facade".equals(normalize(file.classRole()));
    }

    private boolean isEntryPoint(ContextSnapshot file) {
        return "Controller".equals(normalize(file.classRole()))
                || "EntryPointLike".equals(normalize(file.architectureAffinity()))
                || !safeList(file.endpoints()).isEmpty();
    }

    private List<String> limitSamples(Set<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(SAMPLE_LIMIT)
                .toList();
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

    private String formatCount(int count, String singularNoun) {
        return count + " " + (count == 1 ? singularNoun : singularNoun + "s");
    }

    private String formatCountWithVerb(int count, String singularNoun, String singularVerb, String pluralVerb) {
        return formatCount(count, singularNoun) + " " + (count == 1 ? singularVerb : pluralVerb);
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

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value.trim();
    }

    private record RawInputBoundaryEvidence(
            int rawParamUseCaseCount,
            List<String> rawParamUseCaseSamples,
            int useCaseParsingCount,
            List<String> useCaseParsingSamples,
            int controllerParserCount,
            List<String> controllerParserSamples,
            int typedFacadeCount,
            List<String> typedFacadeSamples
    ) {
    }

    private record MethodParameter(String type, String name) {
    }
}
