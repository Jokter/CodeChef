package ai.deep.minicodex.tool;

import ai.deep.minicodex.model.ToolCall;

public interface Tool {
    String name();

    String description();

    ToolResult execute(ToolCall toolCall);
}
