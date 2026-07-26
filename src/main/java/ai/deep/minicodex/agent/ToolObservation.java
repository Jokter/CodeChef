package ai.deep.minicodex.agent;

import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.tool.api.ToolResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次工具调用形成的观察结果。
 *
 * @param toolName 工具名称
 * @param arguments 工具调用参数
 * @param success 工具是否执行成功
 * @param content 工具返回内容或错误说明
 */
public record ToolObservation(
        String toolName,
        Map<String, String> arguments,
        boolean success,
        String content
) {
    public ToolObservation {
        arguments = Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
    }

    /**
     * 根据工具调用和工具结果创建观察结果。
     *
     * @param toolCall 工具调用
     * @param toolResult 工具执行结果
     * @return 工具观察结果
     */
    public static ToolObservation from(ToolCall toolCall, ToolResult toolResult) {
        return new ToolObservation(
                toolCall.name(),
                toolCall.arguments(),
                toolResult.success(),
                toolResult.content()
        );
    }
}
