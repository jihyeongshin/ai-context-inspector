package io.github.jihyeongshin.aicontextinspector.model.guidance;

import java.util.List;

public record ReadingGuidanceSummary(
        List<String> summaryLines,
        List<GuidanceSignal> signals,
        List<String> notes
) {
    public ReadingGuidanceSummary {
        summaryLines = summaryLines == null ? List.of() : List.copyOf(summaryLines);
        signals = signals == null ? List.of() : List.copyOf(signals);
        notes = notes == null ? List.of() : List.copyOf(notes);
    }
}
