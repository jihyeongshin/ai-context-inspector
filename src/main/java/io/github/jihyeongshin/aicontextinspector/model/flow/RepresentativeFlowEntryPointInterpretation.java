package io.github.jihyeongshin.aicontextinspector.model.flow;

import java.util.List;

public record RepresentativeFlowEntryPointInterpretation(
        EntryPointInterpretation interpretation,
        List<String> notes
) {
    private static final int NOTE_LIMIT = 3;

    public RepresentativeFlowEntryPointInterpretation {
        interpretation = interpretation == null ? EntryPointInterpretation.FOCUSED : interpretation;
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
