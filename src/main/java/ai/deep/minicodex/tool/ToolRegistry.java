package ai.deep.minicodex.tool;

import ai.deep.minicodex.model.ToolCall;

import java.util.LinkedHashMap;
import java.util.Map;

public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public ToolResult execute(ToolCall toolCall) {
        Tool tool = tools.get(toolCall.name());
        if (tool == null) {
            return ToolResult.error("未知工具: " + toolCall.name());
        }

        try {
            return tool.execute(toolCall);
        } catch (Exception e) {
            return ToolResult.error("工具执行失败: " + e.getMessage());
        }
    }
}
