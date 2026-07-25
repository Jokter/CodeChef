package ai.deep.minicodex.agent;

import ai.deep.minicodex.model.api.ModelClient;
import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.tool.api.ToolResult;
import ai.deep.minicodex.tool.registry.ToolRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 主循环控制器。
 *
 * <p>该类连接模型客户端与工具注册表。每一轮先把用户任务和历史观察结果交给模型，
 * 再根据模型响应决定是返回最终答案，还是执行一次工具调用并记录观察结果。</p>
 */
public class AgentLoop {
    private static final int MAX_STEPS = 5;

    private final ModelClient modelClient;
    private final ToolRegistry toolRegistry;

    /**
     * 创建 Agent 主循环。
     *
     * @param modelClient 模型客户端，用于生成最终回答或工具调用
     * @param toolRegistry 工具注册表，用于根据工具名执行工具
     */
    public AgentLoop(ModelClient modelClient, ToolRegistry toolRegistry) {
        this.modelClient = modelClient;
        this.toolRegistry = toolRegistry;
    }

    /**
     * 执行用户任务，直到模型给出最终回答或达到最大循环次数。
     *
     * @param userTask 用户输入的自然语言任务
     * @return Agent 的最终回答；若未完成则返回超时提示
     */
    public String run(String userTask) {
        List<String> observations = new ArrayList<>();

        for (int step = 1; step <= MAX_STEPS; step++) {
            ModelResponse response = modelClient.next(userTask, observations);

            if (response.isFinalAnswer()) {
                return response.finalAnswer();
            }

            ToolCall toolCall = response.toolCall();
            System.out.println("第 " + step + " 步，模型请求工具: " + toolCall.name());

            ToolResult toolResult = toolRegistry.execute(toolCall);
            String observation = formatObservation(toolCall, toolResult);
            observations.add(observation);

            System.out.println("工具结果:");
            System.out.println(toolResult.content());
            System.out.println();
        }

        return "达到最大循环次数，任务还没有完成。";
    }

    /**
     * 将工具调用和工具结果格式化为模型下一轮可读取的观察文本。
     *
     * @param toolCall 本轮执行的工具调用
     * @param toolResult 工具执行结果
     * @return 格式化后的观察文本
     */
    private String formatObservation(ToolCall toolCall, ToolResult toolResult) {
        return """
                工具: %s
                成功: %s
                结果:
                %s
                """.formatted(toolCall.name(), toolResult.success(), toolResult.content());
    }
}
