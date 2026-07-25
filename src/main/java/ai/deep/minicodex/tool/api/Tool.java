package ai.deep.minicodex.tool.api;

import ai.deep.minicodex.model.api.ToolCall;

/**
 * Agent 可调用工具的统一接口。
 *
 * <p>工具负责执行具体能力，例如列目录或读取文件。Agent 通过工具名称分发调用，
 * 不直接依赖具体实现类。</p>
 */
public interface Tool {
    /**
     * 工具名称。
     *
     * @return 供模型调用和注册表查找使用的唯一名称
     */
    String name();

    /**
     * 工具说明。
     *
     * @return 面向模型或开发者的简短能力描述
     */
    String description();

    /**
     * 执行工具调用。
     *
     * @param toolCall 模型请求的工具调用，包含名称和参数
     * @return 工具执行结果
     */
    ToolResult execute(ToolCall toolCall);
}
