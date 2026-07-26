package ai.deep.minicodex.tool.api;

import java.util.Map;

/**
 * 工具面向模型的最小说明结构。
 *
 * @param name 工具名称
 * @param description 工具能力说明
 * @param parameters 参数名到自然语言说明的映射
 */
public record ToolSchema(
        String name,
        String description,
        Map<String, String> parameters
) {
}
