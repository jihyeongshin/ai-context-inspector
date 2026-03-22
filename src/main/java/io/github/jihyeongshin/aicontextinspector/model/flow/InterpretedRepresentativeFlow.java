package io.github.jihyeongshin.aicontextinspector.model.flow;

import java.util.List;

public record InterpretedRepresentativeFlow(
        RepresentativeFlow flow,
        RepresentativeFlowMetadata metadata
) {
    public InterpretedRepresentativeFlow {
        if (flow == null) {
            throw new IllegalArgumentException("flow must not be null");
        }
        metadata = metadata == null
                ? new RepresentativeFlowMetadata(FlowConfidence.MEDIUM, FlowAmbiguity.POSSIBLE, List.of())
                : metadata;
    }
}
