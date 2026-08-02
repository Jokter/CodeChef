package ai.deep.minicodex.model.context;

import ai.deep.minicodex.agent.ToolObservation;
import ai.deep.minicodex.model.api.ModelContext;
import ai.deep.minicodex.tool.api.ToolSchema;
import ai.deep.minicodex.tool.api.ToolSchemaRenderer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 构建发给模型看的上下文文本。
 *
 * <p>该类只负责决定模型能看到什么，不负责发送请求、解析模型输出或执行工具。</p>
 */
public class ContextBuilder {
    private static final int COMPACTION_THRESHOLD = 3;
    private static final int RECENT_OBSERVATION_COUNT = 2;
    private static final int MAX_COMPACTED_HISTORY_LENGTH = 1200;
    private static final int MAX_COMPACTED_RESULT_LENGTH = 240;

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
        return SYSTEM_PROMPT_TEMPLATE.formatted(ToolSchemaRenderer.render(toolSchemas));
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

        if (observations.size() <= COMPACTION_THRESHOLD) {
            return "历史工具观察结果:\n" + renderObservations(observations);
        }

        int recentStart = observations.size() - RECENT_OBSERVATION_COUNT;
        List<ToolObservation> olderObservations = observations.subList(0, recentStart);
        List<ToolObservation> recentObservations = observations.subList(recentStart, observations.size());
        return "已压缩的历史观察:\n"
                + buildCompactedHistory(olderObservations)
                + "\n---\n最近工具观察:\n"
                + renderObservations(recentObservations);
    }

    private String renderObservations(List<ToolObservation> observations) {
        return observations.stream()
                .map(this::renderObservation)
                .collect(Collectors.joining("\n---\n"));
    }

    private String buildCompactedHistory(List<ToolObservation> observations) {
        StringBuilder summary = new StringBuilder();
        for (int index = 0; index < observations.size(); index++) {
            String separator = summary.isEmpty() ? "" : "\n---\n";
            String observation = renderCompactedObservation(observations.get(index));
            int remainingCount = observations.size() - index - 1;
            String omittedMessage = remainingCount == 0 ? "" : omittedMessage(remainingCount);
            int availableLength = MAX_COMPACTED_HISTORY_LENGTH - summary.length() - omittedMessage.length();

            if (separator.length() + observation.length() <= availableLength) {
                summary.append(separator).append(observation);
                continue;
            }

            if (availableLength > separator.length()) {
                summary.append(separator);
                summary.append(abbreviate(observation, availableLength - separator.length()));
            }
            summary.append(omittedMessage);
            return summary.toString();
        }
        return summary.toString();
    }

    private String renderCompactedObservation(ToolObservation observation) {
        return """
                工具: %s
                成功: %s
                结果: %s
                """.formatted(
                observation.toolName(),
                observation.success(),
                abbreviate(observation.content(), MAX_COMPACTED_RESULT_LENGTH)
        );
    }

    private String omittedMessage(int omittedCount) {
        return "\n其余 " + omittedCount + " 条旧记录已省略。";
    }

    private String abbreviate(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        if (maxLength <= 3) {
            return content.substring(0, maxLength);
        }
        return content.substring(0, maxLength - 3) + "...";
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
