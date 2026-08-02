package ai.deep.minicodex.cli;

import ai.deep.minicodex.agent.AgentLoop;
import ai.deep.minicodex.model.config.GptModelConfig;
import ai.deep.minicodex.safety.ApprovalService;
import ai.deep.minicodex.tool.api.Tool;
import ai.deep.minicodex.tool.api.ToolResult;
import ai.deep.minicodex.tool.api.ToolSchema;
import ai.deep.minicodex.tool.registry.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlashCommandHandlerTest {
    @TempDir
    Path tempDir;

    @Test
    void showsStatusWithCurrentRuntimeInformation() {
        SlashCommandHandler.CommandResult result = handler().handle("/status");

        assertEquals(SlashCommandHandler.Action.CONTINUE, result.action());
        assertTrue(result.output().contains("当前模型: test-model"));
        assertTrue(result.output().contains("工作区: " + tempDir.toAbsolutePath().normalize()));
        assertTrue(result.output().contains("工具数量: 1"));
        assertTrue(result.output().contains("最大轮数: " + AgentLoop.maxSteps()));
    }

    @Test
    void showsTheSameToolSchemaUsedByModelContext() {
        SlashCommandHandler.CommandResult result = handler().handle("/tools");

        assertEquals(SlashCommandHandler.Action.CONTINUE, result.action());
        assertTrue(result.output().contains("- sample_tool: 测试工具。"));
        assertTrue(result.output().contains("path: 测试路径。"));
    }

    @Test
    void showsApprovalPolicyWithoutDuplicatingIt() {
        SlashCommandHandler.CommandResult result = handler().handle("/permissions");

        assertEquals(SlashCommandHandler.Action.CONTINUE, result.action());
        assertTrue(result.output().contains("read_file、list_files：直接允许"));
        assertTrue(result.output().contains("write_file、run_command：需要确认"));
        assertTrue(result.output().contains("其他工具：拒绝"));
    }

    @Test
    void exitsOnlyForExactExitCommand() {
        SlashCommandHandler.CommandResult result = handler().handle(" /exit ");

        assertEquals(SlashCommandHandler.Action.EXIT, result.action());
        assertEquals("已退出。", result.output());
    }

    @Test
    void keepsTasksOutsideControlPlane() {
        assertEquals(SlashCommandHandler.Action.NOT_A_COMMAND, handler().handle("查看 README").action());
        assertEquals(SlashCommandHandler.Action.CONTINUE, handler().handle("   ").action());

        SlashCommandHandler.CommandResult unknown = handler().handle("/status verbose");
        assertEquals(SlashCommandHandler.Action.CONTINUE, unknown.action());
        assertTrue(unknown.output().contains("未知控制命令"));
    }

    private SlashCommandHandler handler() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new FixedTool());
        return new SlashCommandHandler(
                new GptModelConfig("test-model", "https://example.com", "openai"),
                tempDir,
                registry,
                new ApprovalService(toolCall -> false),
                AgentLoop.maxSteps()
        );
    }

    private static class FixedTool implements Tool {
        @Override
        public String name() {
            return "sample_tool";
        }

        @Override
        public String description() {
            return "测试工具。";
        }

        @Override
        public ToolSchema schema() {
            return new ToolSchema(name(), description(), Map.of("path", "测试路径。"));
        }

        @Override
        public ToolResult execute(ai.deep.minicodex.model.api.ToolCall toolCall) {
            return ToolResult.ok("完成");
        }
    }
}
