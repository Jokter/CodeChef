package ai.deep.minicodex.agent;

import ai.deep.minicodex.model.ModelClient;
import ai.deep.minicodex.model.ModelResponse;
import ai.deep.minicodex.model.ToolCall;
import ai.deep.minicodex.tool.ToolRegistry;
import ai.deep.minicodex.tool.ToolResult;

import java.util.ArrayList;
import java.util.List;

public class AgentLoop {
    private static final int MAX_STEPS = 5;

    private final ModelClient modelClient;
    private final ToolRegistry toolRegistry;

    public AgentLoop(ModelClient modelClient, ToolRegistry toolRegistry) {
        this.modelClient = modelClient;
        this.toolRegistry = toolRegistry;
    }

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

    private String formatObservation(ToolCall toolCall, ToolResult toolResult) {
        return """
                工具: %s
                成功: %s
                结果:
                %s
                """.formatted(toolCall.name(), toolResult.success(), toolResult.content());
    }
}
