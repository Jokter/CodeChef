package ai.deep.minicodex.model.context;

import ai.deep.minicodex.agent.ToolObservation;
import ai.deep.minicodex.model.api.ModelContext;
import ai.deep.minicodex.tool.api.ToolSchema;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 构建发给模型看的上下文文本。
 *
 * <p>该类只负责决定模型能看到什么，不负责发送请求、解析模型输出或执行工具。</p>
 */
public class ContextBuilder {
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是一个最小 Java Agent 的 GPT 模型客户端。
            你必须只返回一个 JSON 对象，不要使用 Markdown 代码块。

            可用工具：
            %s

            返回格式只能是以下两种之一：
            {"type":"final","answer":"最终回答"}
            {"type":"tool_call","tool":"工具名","arguments":{"参数名":"参数值"}}

            当需要查看文件或目录后才能回答时，先返回 tool_call。
            当已经可以回答用户任务时，返回 final。
            """;

    private final List<ToolSchema> toolSchemas;

    /**
     * 创建上下文构建器。
     *
     * @param toolSchemas 可用工具说明列表
     */
    public ContextBuilder(List<ToolSchema> toolSchemas) {
        this.toolSchemas = List.copyOf(toolSchemas);
    }

    /**
     * 构建一次模型请求上下文。
     *
     * @param userTask 用户原始任务
     * @param observations 历史工具观察结果
     * @return 模型请求上下文
     */
    public ModelContext build(String userTask, List<ToolObservation> observations) {
        return new ModelContext(
                buildSystemPrompt(),
                buildUserContent(userTask, observations)
        );
    }

    private String buildSystemPrompt() {
        return SYSTEM_PROMPT_TEMPLATE.formatted(renderToolSchemas());
    }

    private String renderToolSchemas() {
        if (toolSchemas.isEmpty()) {
            return "当前没有可用工具。";
        }

        return toolSchemas.stream()
                .map(this::renderToolSchema)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String renderToolSchema(ToolSchema schema) {
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

    private String buildUserContent(String userTask, List<ToolObservation> observations) {
        return "【用户任务开始】\n"
                + userTask
                + "\n【用户任务结束】\n\n"
                + "【工具观察结果开始】\n"
                + buildObservationContent(observations)
                + "\n【工具观察结果结束】";
    }

    private String buildObservationContent(List<ToolObservation> observations) {
        if (observations.isEmpty()) {
            return "当前还没有工具观察结果。";
        }

        String renderedObservations = observations.stream()
                .map(this::renderObservation)
                .collect(Collectors.joining("\n---\n"));
        return "历史工具观察结果:\n" + renderedObservations;
    }

    private String renderObservation(ToolObservation observation) {
        return """
                工具: %s
                参数: %s
                成功: %s
                结果:
                %s
                """.formatted(
                observation.toolName(),
                observation.arguments(),
                observation.success(),
                observation.content()
        );
    }
}
