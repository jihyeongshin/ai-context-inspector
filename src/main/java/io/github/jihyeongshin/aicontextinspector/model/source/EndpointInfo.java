package io.github.jihyeongshin.aicontextinspector.model.source;

import org.jetbrains.annotations.NotNull;

public record EndpointInfo(
        String httpMethod,
        String path,
        String handlerMethodName
) {
    public String toDisplayString() {
        return httpMethod + " " + path + " -> " + handlerMethodName;
    }

    @Override
    public @NotNull String toString() {
        return toDisplayString();
    }

}

