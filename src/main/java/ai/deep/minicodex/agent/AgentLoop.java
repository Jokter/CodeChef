package ai.deep.minicodex.agent;

import ai.deep.minicodex.model.api.ModelClient;
import ai.deep.minicodex.model.api.ModelContext;
import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.model.context.ContextBuilder;
import ai.deep.minicodex.safety.ApprovalService;
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
    private final ContextBuilder contextBuilder;
    private final ToolRegistry toolRegistry;
    private final ApprovalService approvalService;

    /**
     * 创建带审批服务的 Agent 主循环。
     *
     * @param modelClient 模型客户端
     * @param contextBuilder 上下文构建器
     * @param toolRegistry 工具注册表
     * @param approvalService 审批服务
     */
    public AgentLoop(
            ModelClient modelClient,
            ContextBuilder contextBuilder,
            ToolRegistry toolRegistry,
            ApprovalService approvalService
    ) {
        this.modelClient = modelClient;
        this.contextBuilder = contextBuilder;
        this.toolRegistry = toolRegistry;
        this.approvalService = approvalService;
    }

    /**
     * 执行用户任务，直到模型给出最终回答或达到最大循环次数。
     *
     * @param userTask 用户输入的自然语言任务
     * @return Agent 的最终回答；若未完成则返回超时提示
     */
    public String run(String userTask) {
        List<ToolObservation> observations = new ArrayList<>();

        System.out.println("Agent 开始执行，最大循环次数: " + MAX_STEPS);
        for (int step = 1; step <= MAX_STEPS; step++) {
            System.out.println();
            System.out.println("========== Agent 第 " + step + " 轮 ==========");
            System.out.println("历史观察数量: " + observations.size());

            ModelContext context = contextBuilder.build(userTask, observations);
            ModelResponse response = modelClient.next(context);

            if (response.isFinalAnswer()) {
                System.out.println("模型决策: final");
                System.out.println("本轮生成最终回答，Agent 结束。");
                return response.finalAnswer();
            }

            ToolCall toolCall = response.toolCall();
            System.out.println("模型决策: tool_call");
            System.out.println("工具名称: " + toolCall.name());
            System.out.println("工具参数: " + toolCall.arguments());

            ToolResult toolResult = executeWithApproval(toolCall);
            observations.add(ToolObservation.from(toolCall, toolResult));

            System.out.println("工具执行: " + (toolResult.success() ? "成功" : "失败"));
            System.out.println("工具结果长度: " + toolResult.content().length() + " 字符");
            if (!toolResult.success()) {
                System.out.println("错误内容:");
                System.out.println(toolResult.content());
            }
            System.out.println("观察结果已记录，将在下一轮发送给模型。");
        }

        System.out.println();
        System.out.println("Agent 达到最大循环次数，未收到最终回答。");
        return "达到最大循环次数，任务还没有完成。";
    }

    private ToolResult executeWithApproval(ToolCall toolCall) {
        if (!approvalService.approve(toolCall)) {
            return ToolResult.error("工具调用被拒绝: " + toolCall.name());
        }
        return toolRegistry.execute(toolCall);
    }
}
