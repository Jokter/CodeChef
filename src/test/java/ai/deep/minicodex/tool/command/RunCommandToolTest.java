package ai.deep.minicodex.tool.command;

import ai.deep.minicodex.agent.AgentLoop;
import ai.deep.minicodex.model.api.ModelClient;
import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.model.context.ContextBuilder;
import ai.deep.minicodex.safety.ApprovalService;
import ai.deep.minicodex.safety.WorkspacePolicy;
import ai.deep.minicodex.tool.api.ToolResult;
import ai.deep.minicodex.tool.api.ToolSchema;
import ai.deep.minicodex.tool.registry.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunCommandToolTest {
    @TempDir
    Path tempDir;

    private RecordingRunCommandTool recordingTool;

    @Test
    void executesWhitelistedMavenCommandInWorkspace() {
        ToolResult result = tool(new FakeProcess("编译完成", "", 0, false))
                .execute(call("mvn compile"));

        assertTrue(result.success());
        assertEquals(List.of("mvn.cmd", "compile"), recordingTool.command);
        assertEquals(tempDir.toAbsolutePath().normalize(), recordingTool.workingDirectory);
        assertTrue(result.content().contains("退出码: 0"));
        assertTrue(result.content().contains("stdout:\n编译完成"));
        assertTrue(result.content().contains("stderr:\n"));
    }

    @Test
    void allowsRgWithWorkspaceRelativePath() {
        ToolResult result = tool(new FakeProcess("src/main/java/App.java", "", 0, false))
                .execute(call("rg TODO src"));

        assertTrue(result.success());
        assertEquals(List.of("rg", "TODO", "src"), recordingTool.command);
    }

    @Test
    void allowsRgFilesListing() {
        ToolResult result = tool(new FakeProcess("README.md", "", 0, false))
                .execute(call("rg --files"));

        assertTrue(result.success());
        assertEquals(List.of("rg", "--files"), recordingTool.command);
    }

    @Test
    void rejectsCommandsOutsideWhitelistOrWithShellSyntax() {
        assertRejected("cmd /c dir");
        assertRejected("git log");
        assertRejected("mvn test & whoami");
        assertRejected("rg TODO ../outside");
    }

    @Test
    void rejectsInvalidTimeout() {
        assertTimeoutRejected("0");
        assertTimeoutRejected("121");
        assertTimeoutRejected("fast");
    }

    @Test
    void usesThirtySecondsWhenTimeoutIsOmitted() {
        FakeProcess process = new FakeProcess("", "", 0, false);
        ToolResult result = tool(process).execute(call("git status"));

        assertTrue(result.success());
        assertEquals(30, process.timeoutSeconds);
    }

    @Test
    void marksNonZeroExitCodeAsFailureAndPreservesBothOutputs() {
        ToolResult result = tool(new FakeProcess("测试失败", "断言失败", 1, false))
                .execute(call("mvn test"));

        assertFalse(result.success());
        assertTrue(result.content().contains("退出码: 1"));
        assertTrue(result.content().contains("stdout:\n测试失败"));
        assertTrue(result.content().contains("stderr:\n断言失败"));
    }

    @Test
    void terminatesProcessAfterTimeout() {
        FakeProcess process = new FakeProcess("", "", 1, true);
        ToolResult result = tool(process).execute(call("git status", "1"));

        assertFalse(result.success());
        assertTrue(process.destroyed);
        assertEquals(1, process.timeoutSeconds);
        assertTrue(result.content().contains("命令执行超时，已终止"));
    }

    @Test
    void truncatesOutputAfterSixtyFourKiB() {
        String oversizedOutput = "x".repeat(64 * 1024 + 1);
        ToolResult result = tool(new FakeProcess(oversizedOutput, "", 0, false))
                .execute(call("git diff"));

        assertTrue(result.success());
        assertTrue(result.content().contains("[stdout 已截断]"));
    }

    @Test
    void providesSchemaForCommandAndTimeout() {
        ToolSchema schema = tool(new FakeProcess("", "", 0, false)).schema();

        assertEquals("run_command", schema.name());
        assertTrue(schema.parameters().containsKey("command"));
        assertTrue(schema.parameters().containsKey("timeoutSeconds"));
    }

    @Test
    void executesRunCommandThroughAgentAfterApproval() {
        FakeProcess process = new FakeProcess("测试通过", "", 0, false);
        RunCommandTool commandTool = tool(process);
        ToolRegistry registry = new ToolRegistry();
        registry.register(commandTool);
        AgentLoop agentLoop = new AgentLoop(
                new OneCommandThenFinalClient(),
                new ContextBuilder(registry.schemas()),
                registry,
                new ApprovalService(toolCall -> true)
        );

        String answer = agentLoop.run("运行测试");

        assertEquals("测试已完成", answer);
        assertNotNull(recordingTool.command);
        assertEquals(List.of("mvn.cmd", "test"), recordingTool.command);
    }

    private RunCommandTool tool(FakeProcess process) {
        recordingTool = new RecordingRunCommandTool(new WorkspacePolicy(tempDir), process);
        return recordingTool;
    }

    private ToolCall call(String command) {
        return new ToolCall("run_command", Map.of("command", command));
    }

    private ToolCall call(String command, String timeoutSeconds) {
        return new ToolCall("run_command", Map.of(
                "command", command,
                "timeoutSeconds", timeoutSeconds
        ));
    }

    private void assertRejected(String command) {
        ToolResult result = tool(new FakeProcess("", "", 0, false)).execute(call(command));

        assertFalse(result.success());
        assertNull(recordingTool.command);
    }

    private void assertTimeoutRejected(String timeoutSeconds) {
        ToolResult result = tool(new FakeProcess("", "", 0, false))
                .execute(call("git status", timeoutSeconds));

        assertFalse(result.success());
        assertNull(recordingTool.command);
    }

    private static class OneCommandThenFinalClient implements ModelClient {
        private int calls;

        @Override
        public ModelResponse next(ai.deep.minicodex.model.api.ModelContext context) {
            calls++;
            if (calls == 1) {
                return ModelResponse.toolCall(new ToolCall("run_command", Map.of("command", "mvn test")));
            }
            return ModelResponse.finalAnswer("测试已完成");
        }
    }

    private static class RecordingRunCommandTool extends RunCommandTool {
        private final Process process;
        private List<String> command;
        private Path workingDirectory;

        private RecordingRunCommandTool(WorkspacePolicy workspacePolicy, Process process) {
            super(workspacePolicy);
            this.process = process;
        }

        @Override
        Process startProcess(List<String> command, Path workingDirectory) {
            this.command = List.copyOf(command);
            this.workingDirectory = workingDirectory;
            return process;
        }
    }

    private static class FakeProcess extends Process {
        private final InputStream stdout;
        private final InputStream stderr;
        private final int exitCode;
        private boolean alive;
        private boolean destroyed;
        private long timeoutSeconds;

        private FakeProcess(String stdout, String stderr, int exitCode, boolean alive) {
            this.stdout = new ByteArrayInputStream(stdout.getBytes(StandardCharsets.UTF_8));
            this.stderr = new ByteArrayInputStream(stderr.getBytes(StandardCharsets.UTF_8));
            this.exitCode = exitCode;
            this.alive = alive;
        }

        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return stdout;
        }

        @Override
        public InputStream getErrorStream() {
            return stderr;
        }

        @Override
        public int waitFor() {
            alive = false;
            return exitCode;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            if (timeoutSeconds == 0) {
                timeoutSeconds = unit.toSeconds(timeout);
            }
            return !alive;
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("进程仍在运行");
            }
            return exitCode;
        }

        @Override
        public void destroy() {
            destroyed = true;
            alive = false;
        }

        @Override
        public Process destroyForcibly() {
            destroy();
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }
    }
}
