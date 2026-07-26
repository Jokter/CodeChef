package ai.deep.minicodex.agent;

import ai.deep.minicodex.model.api.ModelClient;
import ai.deep.minicodex.model.api.ModelContext;
import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.model.context.ContextBuilder;
import ai.deep.minicodex.tool.api.Tool;
import ai.deep.minicodex.tool.api.ToolResult;
import ai.deep.minicodex.tool.api.ToolSchema;
import ai.deep.minicodex.tool.registry.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentLoopTest {
    @Test
    void sendsStructuredToolObservationToNextModelTurn() {
        CapturingModelClient modelClient = new CapturingModelClient();
        CapturingContextBuilder contextBuilder = new CapturingContextBuilder();
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new FixedTool("read_file", ToolResult.ok("文件内容")));
        AgentLoop agentLoop = new AgentLoop(
                modelClient,
                contextBuilder,
                toolRegistry
        );

        String answer = agentLoop.run("读取 README");

        ToolObservation observation = contextBuilder.secondTurnObservations.getFirst();
        assertEquals("完成", answer);
        assertEquals("read_file", observation.toolName());
        assertEquals(Map.of("path", "README.md"), observation.arguments());
        assertTrue(observation.success());
        assertEquals("文件内容", observation.content());
        assertTrue(modelClient.secondContext.userContent().contains("工具: read_file"));
        assertTrue(modelClient.secondContext.userContent().contains("参数: {path=README.md}"));
    }

    private static class CapturingContextBuilder extends ContextBuilder {
        private int calls;
        private List<ToolObservation> secondTurnObservations;

        private CapturingContextBuilder() {
            super(List.of());
        }

        @Override
        public ModelContext build(String userTask, List<ToolObservation> observations) {
            calls++;
            if (calls == 2) {
                secondTurnObservations = observations;
            }
            return super.build(userTask, observations);
        }
    }

    private static class CapturingModelClient implements ModelClient {
        private int calls;
        private ModelContext secondContext;

        @Override
        public ModelResponse next(ModelContext context) {
            calls++;
            if (calls == 1) {
                return ModelResponse.toolCall(new ToolCall("read_file", Map.of("path", "README.md")));
            }

            secondContext = context;
            return ModelResponse.finalAnswer("完成");
        }
    }

    private record FixedTool(String name, ToolResult result) implements Tool {
        @Override
        public String description() {
            return "固定返回结果的测试工具。";
        }

        @Override
        public ToolSchema schema() {
            return new ToolSchema(name(), description(), Map.of(
                    "path", "相对工作区的文件路径。"
            ));
        }

        @Override
        public ToolResult execute(ToolCall toolCall) {
            return result;
        }
    }
}
