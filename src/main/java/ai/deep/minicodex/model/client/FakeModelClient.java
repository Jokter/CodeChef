package ai.deep.minicodex.model.client;

import ai.deep.minicodex.model.api.ModelClient;
import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;

import java.util.List;
import java.util.Map;

/**
 * 用于演示的假模型客户端。
 *
 * <p>该实现不调用真实模型 API，而是用简单规则模拟模型行为。第一轮根据用户任务
 * 选择一个工具调用；后续轮次根据最近一次观察结果生成最终回答。</p>
 */
public class FakeModelClient implements ModelClient {
    /**
     * 根据用户任务和历史观察结果返回下一步模型响应。
     *
     * @param userTask 用户原始任务
     * @param observations 已有工具观察结果；为空表示尚未执行过工具
     * @return 工具调用请求或最终回答
     */
    @Override
    public ModelResponse next(String userTask, List<String> observations) {
        if (observations.isEmpty()) {
            return firstToolCall(userTask);
        }

        String lastObservation = observations.getLast();
        return ModelResponse.finalAnswer("""
                我已经完成一次最小 agent loop。

                你可以从这次运行里看到核心结构：
                1. 模型没有直接读文件。
                2. 模型只是请求工具。
                3. Java 程序负责执行工具。
                4. 工具结果再返回给模型。

                最近一次观察结果如下：
                %s
                """.formatted(lastObservation));
    }

    /**
     * 为第一轮模型调用选择工具。
     *
     * <p>当前规则仅用于教学演示：任务中包含 {@code readme} 或 {@code 读} 时读取
     * README，否则列出工作区根目录。</p>
     *
     * @param userTask 用户任务
     * @return 第一轮工具调用响应
     */
    private ModelResponse firstToolCall(String userTask) {
        String normalizedTask = userTask.toLowerCase();
        if (normalizedTask.contains("readme") || normalizedTask.contains("读")) {
            return ModelResponse.toolCall(new ToolCall("read_file", Map.of("path", "README.md")));
        }

        return ModelResponse.toolCall(new ToolCall("list_files", Map.of("path", ".")));
    }
}
