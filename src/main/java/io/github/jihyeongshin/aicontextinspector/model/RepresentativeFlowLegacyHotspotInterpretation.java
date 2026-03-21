package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;

public record RepresentativeFlowLegacyHotspotInterpretation(
        LegacyHotspotLevel legacyHotspot,
        List<String> hotspotNotes
) {
    private static final int NOTE_LIMIT = 3;

    public RepresentativeFlowLegacyHotspotInterpretation {
        legacyHotspot = legacyHotspot == null ? LegacyHotspotLevel.NONE : legacyHotspot;
        hotspotNotes = legacyHotspot == LegacyHotspotLevel.NONE || hotspotNotes == null
                ? List.of()
                : hotspotNotes.stream()
                .filter(note -> note != null && !note.isBlank())
                .distinct()
                .limit(NOTE_LIMIT)
                .toList();
    }

    public String hotspotNotesDisplayString() {
        if (hotspotNotes.isEmpty()) {
            return "None";
        }
        return String.join(", ", hotspotNotes);
    }
}
