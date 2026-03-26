package io.github.jihyeongshin.aicontextinspector.model.guidance;

import java.util.List;

public record RawInputBoundaryGuidanceSummary(
        List<GuidanceSignal> signals,
        List<String> notes
) {
    public RawInputBoundaryGuidanceSummary {
        signals = signals == null ? List.of() : List.copyOf(signals);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
