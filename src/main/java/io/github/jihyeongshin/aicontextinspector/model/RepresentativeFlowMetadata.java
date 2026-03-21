package io.github.jihyeongshin.aicontextinspector.model;

import java.util.List;

public record RepresentativeFlowMetadata(
        FlowConfidence confidence,
        FlowAmbiguity ambiguity,
        List<String> notes
) {
    private static final int NOTE_LIMIT = 3;

    public RepresentativeFlowMetadata {
        confidence = confidence == null ? FlowConfidence.MEDIUM : confidence;
        ambiguity = ambiguity == null ? FlowAmbiguity.POSSIBLE : ambiguity;
        notes = notes == null
                ? List.of()
                : notes.stream()
                .filter(note -> note != null && !note.isBlank())
                .distinct()
                .limit(NOTE_LIMIT)
                .toList();
    }

    public String notesDisplayString() {
        if (notes.isEmpty()) {
            return "None";
        }
        return String.join(", ", notes);
    }
}
