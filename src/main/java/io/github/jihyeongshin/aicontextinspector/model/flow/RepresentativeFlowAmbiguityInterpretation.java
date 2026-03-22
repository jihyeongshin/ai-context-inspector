package io.github.jihyeongshin.aicontextinspector.model.flow;

import java.util.List;

public record RepresentativeFlowAmbiguityInterpretation(
        FlowAmbiguity ambiguity,
        List<String> notes
) {
    private static final int NOTE_LIMIT = 3;

    public RepresentativeFlowAmbiguityInterpretation {
        ambiguity = ambiguity == null ? FlowAmbiguity.NONE : ambiguity;
        notes = ambiguity == FlowAmbiguity.NONE || notes == null
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
