package io.github.jihyeongshin.aicontextinspector.model.guidance;

public record GuidanceSignal(
        String title,
        GuidanceSignalStatus status,
        String message
) {
    public GuidanceSignal {
        title = title == null || title.isBlank() ? "Guidance" : title.trim();
        status = status == null ? GuidanceSignalStatus.NOT_APPLICABLE : status;
        message = message == null || message.isBlank()
                ? status.displayName()
                : message.trim();
    }
}
