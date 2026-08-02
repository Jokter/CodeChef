package ai.deep.minicodex.tool.api;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 将工具 schema 渲染为面向用户或模型的文本。
 */
public final class ToolSchemaRenderer {
    private ToolSchemaRenderer() {
    }

    /**
     * 渲染工具列表及其参数说明。
     *
     * @param schemas 工具 schema 列表
     * @return 可读的工具说明文本
     */
    public static String render(List<ToolSchema> schemas) {
        if (schemas.isEmpty()) {
            return "当前没有可用工具。";
        }

        return schemas.stream()
                .map(ToolSchemaRenderer::renderSchema)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static String renderSchema(ToolSchema schema) {
        String parameters = schema.parameters().entrySet().stream()
                .map(entry -> "  - " + entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
        if (parameters.isBlank()) {
            parameters = "  - 无参数。";
        }

        return "- " + schema.name() + ": " + schema.description()
                + System.lineSeparator()
                + "  参数："
                + System.lineSeparator()
                + parameters;
    }
}
