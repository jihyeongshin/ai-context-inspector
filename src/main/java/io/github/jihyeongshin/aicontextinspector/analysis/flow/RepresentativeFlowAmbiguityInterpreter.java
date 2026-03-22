package io.github.jihyeongshin.aicontextinspector.analysis.flow;

import io.github.jihyeongshin.aicontextinspector.model.flow.EntryPointInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.FlowAmbiguity;
import io.github.jihyeongshin.aicontextinspector.model.flow.InterpretedRepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.snapshot.ProjectContextSnapshot;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlow;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowAmbiguityInterpretation;
import io.github.jihyeongshin.aicontextinspector.model.flow.RepresentativeFlowEntryPointInterpretation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RepresentativeFlowAmbiguityInterpreter {
    private static final String NOTE_COMPETING_REPRESENTATIVE_BRANCH = "competing representative branch";
    private static final String NOTE_PLAUSIBLE_ALTERNATE_SERVICE_BRANCH = "plausible alternate service branch";
    private static final String NOTE_NON_DEFINITIVE_REPRESENTATIVE_PATH = "non-definitive representative path";
    private static final String NOTE_SHARED_SERVICE_HUB_AMBIGUITY = "shared service hub ambiguity";
    private static final String NOTE_MULTI_PURPOSE_ENTRY_POINT_SPILLOVER = "multi-purpose entry point spillover";
    private static final String NOTE_ACCEPTABLE_BUT_BRANCH_SENSITIVE_FLOW = "acceptable but branch-sensitive flow";

    private final RepresentativeFlowEntryPointInterpreter representativeFlowEntryPointInterpreter =
            new RepresentativeFlowEntryPointInterpreter();

    public Map<String, RepresentativeFlowAmbiguityInterpretation> evaluate(
            ProjectContextSnapshot snapshot,
            List<InterpretedRepresentativeFlow> interpretedFlows
    ) {
        if (snapshot == null || snapshot.isEmpty() || interpretedFlows == null || interpretedFlows.isEmpty()) {
            return Map.of();
        }

        List<RepresentativeFlow> flows = interpretedFlows.stream()
                .map(InterpretedRepresentativeFlow::flow)
                .toList();
        Map<String, RepresentativeFlowEntryPointInterpretation> entryPointInterpretations =
                representativeFlowEntryPointInterpreter.evaluate(snapshot, flows);
        Map<String, String> flowEntryPointIdentities = new LinkedHashMap<>();
        Map<String, List<InterpretedRepresentativeFlow>> flowsByEntryPointIdentity = new LinkedHashMap<>();
        Map<String, Set<String>> serviceHubEntryPointIdentities = new LinkedHashMap<>();

        for (InterpretedRepresentativeFlow interpretedFlow : safeInterpretedFlowList(interpretedFlows)) {
            RepresentativeFlow flow = interpretedFlow.flow();
            String entryPointIdentity = representativeFlowEntryPointInterpreter.entryPointIdentity(snapshot, flow);
            flowEntryPointIdentities.put(flow.toDisplayString(), entryPointIdentity);
            flowsByEntryPointIdentity.computeIfAbsent(entryPointIdentity, key -> new ArrayList<>()).add(interpretedFlow);

            for (String serviceClassName : serviceClassNames(flow)) {
                serviceHubEntryPointIdentities
                        .computeIfAbsent(serviceClassName, key -> new LinkedHashSet<>())
                        .add(entryPointIdentity);
            }
        }

        Map<String, RepresentativeFlowAmbiguityInterpretation> interpretations = new LinkedHashMap<>();
        for (InterpretedRepresentativeFlow interpretedFlow : safeInterpretedFlowList(interpretedFlows)) {
            RepresentativeFlow flow = interpretedFlow.flow();
            String entryPointIdentity = flowEntryPointIdentities.getOrDefault(flow.toDisplayString(), "Unknown");
            List<InterpretedRepresentativeFlow> siblingFlows =
                    flowsByEntryPointIdentity.getOrDefault(entryPointIdentity, List.of());
            RepresentativeFlowEntryPointInterpretation entryPointInterpretation =
                    entryPointInterpretations.getOrDefault(
                            entryPointIdentity,
                            new RepresentativeFlowEntryPointInterpretation(null, List.of())
                    );

            interpretations.put(
                    flow.toDisplayString(),
                    interpretFlow(
                            interpretedFlow,
                            siblingFlows,
                            entryPointInterpretation,
                            entryPointIdentity,
                            serviceHubEntryPointIdentities
                    )
            );
        }

        return Map.copyOf(interpretations);
    }

    private RepresentativeFlowAmbiguityInterpretation interpretFlow(
            InterpretedRepresentativeFlow interpretedFlow,
            List<InterpretedRepresentativeFlow> siblingFlows,
            RepresentativeFlowEntryPointInterpretation entryPointInterpretation,
            String entryPointIdentity,
            Map<String, Set<String>> serviceHubEntryPointIdentities
    ) {
        FlowAmbiguity ambiguity = interpretedFlow.metadata().ambiguity();
        if (ambiguity == FlowAmbiguity.NONE) {
            return new RepresentativeFlowAmbiguityInterpretation(ambiguity, List.of());
        }

        RepresentativeFlow flow = interpretedFlow.flow();
        boolean competingRepresentativeBranch = siblingFlows.size() > 1;
        boolean plausibleAlternateServiceBranch = hasPlausibleAlternateServiceBranch(flow, siblingFlows);
        boolean sharedServiceHubAmbiguity =
                hasSharedServiceHubAmbiguity(flow, entryPointIdentity, serviceHubEntryPointIdentities);
        boolean multiPurposeEntryPointSpillover =
                entryPointInterpretation.interpretation() == EntryPointInterpretation.MULTI_PURPOSE;
        boolean acceptableButBranchSensitive =
                ambiguity == FlowAmbiguity.POSSIBLE
                        && (competingRepresentativeBranch
                        || plausibleAlternateServiceBranch
                        || sharedServiceHubAmbiguity);

        List<String> notes = new ArrayList<>();
        if (competingRepresentativeBranch) {
            notes.add(NOTE_COMPETING_REPRESENTATIVE_BRANCH);
        }
        if (plausibleAlternateServiceBranch) {
            notes.add(NOTE_PLAUSIBLE_ALTERNATE_SERVICE_BRANCH);
        }
        if (multiPurposeEntryPointSpillover) {
            notes.add(NOTE_MULTI_PURPOSE_ENTRY_POINT_SPILLOVER);
        }
        if (sharedServiceHubAmbiguity) {
            notes.add(NOTE_SHARED_SERVICE_HUB_AMBIGUITY);
        }
        if (acceptableButBranchSensitive) {
            notes.add(NOTE_ACCEPTABLE_BUT_BRANCH_SENSITIVE_FLOW);
        }
        if (notes.isEmpty()) {
            notes.add(NOTE_NON_DEFINITIVE_REPRESENTATIVE_PATH);
        }

        return new RepresentativeFlowAmbiguityInterpretation(ambiguity, notes);
    }

    private boolean hasPlausibleAlternateServiceBranch(
            RepresentativeFlow targetFlow,
            List<InterpretedRepresentativeFlow> siblingFlows
    ) {
        String targetSignature = branchSignature(targetFlow);
        if ("Unknown".equals(targetSignature)) {
            return false;
        }

        for (InterpretedRepresentativeFlow siblingFlow : safeInterpretedFlowList(siblingFlows)) {
            RepresentativeFlow sibling = siblingFlow.flow();
            if (targetFlow.toDisplayString().equals(sibling.toDisplayString())) {
                continue;
            }

            String siblingSignature = branchSignature(sibling);
            if (!"Unknown".equals(siblingSignature) && !targetSignature.equals(siblingSignature)) {
                return true;
            }
        }
        return false;
    }

    private String branchSignature(RepresentativeFlow flow) {
        List<String> classNames = safeList(flow.classNames());
        List<String> classRoles = safeList(flow.classRoles());
        if (classNames.size() < 2) {
            return "Unknown";
        }

        List<String> branchTokens = new ArrayList<>();
        for (int index = 1; index < classNames.size(); index++) {
            String role = index < classRoles.size() ? normalize(classRoles.get(index)) : "Unknown";
            if (isBranchRelevantRole(role)) {
                branchTokens.add(normalizeDomainToken(classNames.get(index)));
            }
        }

        if (branchTokens.isEmpty()) {
            return "Unknown";
        }
        return String.join(" -> ", branchTokens);
    }

    private boolean hasSharedServiceHubAmbiguity(
            RepresentativeFlow flow,
            String entryPointIdentity,
            Map<String, Set<String>> serviceHubEntryPointIdentities
    ) {
        for (String serviceClassName : serviceClassNames(flow)) {
            Set<String> identities = serviceHubEntryPointIdentities.getOrDefault(serviceClassName, Set.of());
            if (identities.size() >= 2 && identities.contains(entryPointIdentity)) {
                return true;
            }
        }
        return false;
    }

    private List<String> serviceClassNames(RepresentativeFlow flow) {
        List<String> classNames = safeList(flow.classNames());
        List<String> classRoles = safeList(flow.classRoles());
        List<String> serviceClassNames = new ArrayList<>();

        for (int index = 0; index < classNames.size(); index++) {
            String role = index < classRoles.size() ? normalize(classRoles.get(index)) : "Unknown";
            if ("Service".equals(role)) {
                serviceClassNames.add(normalize(classNames.get(index)));
            }
        }

        return serviceClassNames;
    }

    private boolean isBranchRelevantRole(String role) {
        return "Facade".equals(role)
                || "UseCase".equals(role)
                || "Service".equals(role)
                || "Repository".equals(role)
                || "Mapper".equals(role)
                || "Client".equals(role)
                || "Adapter".equals(role);
    }

    private String normalizeDomainToken(String className) {
        return normalize(className)
                .replace("Controller", "")
                .replace("Facade", "")
                .replace("UseCase", "")
                .replace("Service", "")
                .replace("Repository", "")
                .replace("Mapper", "")
                .replace("Client", "")
                .replace("Adapter", "")
                .replace("Dispatcher", "")
                .replace("Request", "")
                .replace("Response", "")
                .replace("Dto", "")
                .replace("DTO", "")
                .trim();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<InterpretedRepresentativeFlow> safeInterpretedFlowList(List<InterpretedRepresentativeFlow> values) {
        return values == null ? List.of() : values;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }
}

