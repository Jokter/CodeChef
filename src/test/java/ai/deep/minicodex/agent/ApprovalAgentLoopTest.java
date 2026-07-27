package ai.deep.minicodex.agent;

import ai.deep.minicodex.model.api.ModelClient;
import ai.deep.minicodex.model.api.ModelContext;
import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.model.context.ContextBuilder;
import ai.deep.minicodex.safety.ApprovalService;
import ai.deep.minicodex.tool.api.Tool;
import ai.deep.minicodex.tool.api.ToolResult;
import ai.deep.minicodex.tool.api.ToolSchema;
import ai.deep.minicodex.tool.registry.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalAgentLoopTest {
    @Test
    void deniedWriteToolIsNotExecutedAndObservationIsReturned() {
        AtomicInteger executions = new AtomicInteger();
        CapturingContextBuilder contextBuilder = new CapturingContextBuilder();
        AgentLoop agentLoop = new AgentLoop(
                new OneToolThenFinalClient("write_file"),
                contextBuilder,
                registry("write_file", executions),
                new ApprovalService(toolCall -> false)
        );

        assertEquals("完成", agentLoop.run("写文件"));
        assertEquals(0, executions.get());
        ToolObservation observation = contextBuilder.secondTurnObservations.get(0);
        assertFalse(observation.success());
        assertTrue(observation.content().contains("工具调用被拒绝"));
    }

    @Test
    void approvedWriteToolIsExecuted() {
        AtomicInteger executions = new AtomicInteger();
        CapturingContextBuilder contextBuilder = new CapturingContextBuilder();
        AgentLoop agentLoop = new AgentLoop(
                new OneToolThenFinalClient("write_file"),
                contextBuilder,
                registry("write_file", executions),
                new ApprovalService(toolCall -> true)
        );

        assertEquals("完成", agentLoop.run("写文件"));
        assertEquals(1, executions.get());
        assertTrue(contextBuilder.secondTurnObservations.get(0).success());
    }

    private ToolRegistry registry(String toolName, AtomicInteger executions) {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new CountingTool(toolName, executions));
        return registry;
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

    private record OneToolThenFinalClient(String toolName) implements ModelClient {
        private static final ToolCall CALL = new ToolCall("write_file", Map.of(
                "path", "new.txt",
                "content", "内容"
        ));

        @Override
        public ModelResponse next(ModelContext context) {
            return context.userContent().contains("工具: ")
                    ? ModelResponse.finalAnswer("完成")
                    : ModelResponse.toolCall(new ToolCall(toolName, CALL.arguments()));
        }
    }

    private record CountingTool(String name, AtomicInteger executions) implements Tool {
        @Override
        public String description() {
            return "测试工具。";
        }

        @Override
        public ToolSchema schema() {
            return new ToolSchema(name(), description(), Map.of());
        }

        @Override
        public ToolResult execute(ToolCall toolCall) {
            executions.incrementAndGet();
            return ToolResult.ok("已执行");
        }
    }
}
