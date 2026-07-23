package ai.deep.minicodex.tool;

import ai.deep.minicodex.model.ToolCall;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具注册表。
 *
 * <p>该类负责保存可用工具，并根据模型返回的 {@link ToolCall} 名称查找和执行工具。
 * 它同时承担基础错误隔离：未知工具和工具执行异常都会被转换为失败的
 * {@link ToolResult}，避免异常直接打断 Agent 主循环。</p>
 */
public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    /**
     * 注册一个工具。
     *
     * <p>如果重复注册同名工具，后注册的工具会覆盖先前工具。</p>
     *
     * @param tool 要注册的工具实现
     */
    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    /**
     * 执行模型请求的工具调用。
     *
     * @param toolCall 工具调用，包含工具名和参数
     * @return 工具执行结果；未知工具或执行异常时返回错误结果
     */
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
