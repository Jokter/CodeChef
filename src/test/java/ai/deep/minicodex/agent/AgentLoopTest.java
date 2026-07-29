package ai.deep.minicodex.agent;

import ai.deep.minicodex.model.api.ModelClient;
import ai.deep.minicodex.model.api.ModelContext;
import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.model.context.ContextBuilder;
import ai.deep.minicodex.agent.session.SessionLog;
import ai.deep.minicodex.safety.ApprovalService;
import ai.deep.minicodex.tool.api.Tool;
import ai.deep.minicodex.tool.api.ToolResult;
import ai.deep.minicodex.tool.api.ToolSchema;
import ai.deep.minicodex.tool.registry.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentLoopTest {
    @TempDir
    Path tempDir;

    @Test
    void sendsStructuredToolObservationToNextModelTurn() throws IOException {
        CapturingModelClient modelClient = new CapturingModelClient();
        CapturingContextBuilder contextBuilder = new CapturingContextBuilder();
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new FixedTool("read_file", ToolResult.ok("文件内容")));
        AgentLoop agentLoop = new AgentLoop(
                modelClient,
                contextBuilder,
                toolRegistry,
                new ApprovalService(toolCall -> false),
                new SessionLog(tempDir)
        );

        String answer = agentLoop.run("读取 README");

        ToolObservation observation = contextBuilder.secondTurnObservations.get(0);
        assertEquals("完成", answer);
        assertEquals("read_file", observation.toolName());
        assertEquals(Map.of("path", "README.md"), observation.arguments());
        assertTrue(observation.success());
        assertEquals("文件内容", observation.content());
        assertTrue(modelClient.secondContext.userContent().contains("工具: read_file"));
        assertTrue(modelClient.secondContext.userContent().contains("参数: {path=README.md}"));
        Path logFile = onlyLogFile();
        String logContent = Files.readString(logFile);
        assertTrue(logContent.contains("\"type\":\"user_input\""));
        assertTrue(logContent.contains("\"type\":\"model_response\""));
        assertTrue(logContent.contains("\"type\":\"tool_call\""));
        assertTrue(logContent.contains("\"type\":\"tool_result\""));
        assertTrue(logContent.contains("\"type\":\"final_answer\""));
        assertTrue(logContent.contains("\"type\":\"session_finished\""));
    }

    @Test
    void preservesFailedSessionLogWhenModelThrows() throws IOException {
        AgentLoop agentLoop = new AgentLoop(
                context -> {
                    throw new IllegalStateException("模型不可用");
                },
                new ContextBuilder(List.of()),
                new ToolRegistry(),
                new ApprovalService(toolCall -> false),
                new SessionLog(tempDir)
        );

        assertThrows(IllegalStateException.class, () -> agentLoop.run("执行任务"));

        String logContent = Files.readString(onlyLogFile());
        assertTrue(logContent.contains("\"type\":\"user_input\""));
        assertTrue(logContent.contains("\"type\":\"session_finished\""));
        assertTrue(logContent.contains("\"status\":\"failed\""));
        assertTrue(logContent.contains("java.lang.IllegalStateException"));
    }

    private Path onlyLogFile() throws IOException {
        try (Stream<Path> files = Files.list(tempDir.resolve(".minicodex/sessions"))) {
            return files.findFirst().orElseThrow();
        }
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
