package ai.deep.minicodex.model;

import java.util.List;
import java.util.Map;

public class FakeModelClient implements ModelClient {
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

    private ModelResponse firstToolCall(String userTask) {
        String normalizedTask = userTask.toLowerCase();
        if (normalizedTask.contains("readme") || normalizedTask.contains("读")) {
            return ModelResponse.toolCall(new ToolCall("read_file", Map.of("path", "README.md")));
        }

        return ModelResponse.toolCall(new ToolCall("list_files", Map.of("path", ".")));
    }
}
