package io.github.jihyeongshin.aicontextinspector.model.guidance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ParserVisibilitySummary(
        List<GuidanceSignal> signals,
        List<String> notes,
        Map<String, List<String>> entryPointHelpers
) {
    public ParserVisibilitySummary {
        signals = signals == null ? List.of() : List.copyOf(signals);
        notes = notes == null ? List.of() : List.copyOf(notes);

        Map<String, List<String>> normalizedHelpers = new LinkedHashMap<>();
        if (entryPointHelpers != null) {
            entryPointHelpers.forEach((key, value) -> normalizedHelpers.put(
                    key,
                    value == null ? List.of() : List.copyOf(value)
            ));
        }
        entryPointHelpers = Map.copyOf(normalizedHelpers);
    }
}
