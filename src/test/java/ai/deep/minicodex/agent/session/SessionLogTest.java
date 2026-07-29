package ai.deep.minicodex.agent.session;

import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.tool.api.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionLogTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void writesCompleteJsonlEventChain() throws IOException {
        SessionLog sessionLog = new SessionLog(tempDir);
        Session session = sessionLog.start("运行测试");
        Turn firstTurn = new Turn(1);
        ToolCall toolCall = new ToolCall("run_command", Map.of("command", "mvn test"));

        sessionLog.recordModelResponse(session, firstTurn, ModelResponse.toolCall(toolCall));
        sessionLog.recordToolCall(session, firstTurn, toolCall);
        sessionLog.recordToolResult(session, firstTurn, ToolResult.ok("测试通过"));
        sessionLog.recordModelResponse(session, new Turn(2), ModelResponse.finalAnswer("完成"));
        sessionLog.recordFinalAnswer(session, new Turn(2), "完成");
        sessionLog.finish(session, "completed", null);

        UUID.fromString(session.id());
        List<JsonNode> events = Files.readAllLines(logPath(session), StandardCharsets.UTF_8).stream()
                .map(this::readTree)
                .toList();

        assertEquals(List.of(
                "user_input",
                "model_response",
                "tool_call",
                "tool_result",
                "model_response",
                "final_answer",
                "session_finished"
        ), events.stream().map(event -> event.path("type").asText()).toList());
        assertTrue(events.stream().allMatch(event -> session.id().equals(event.path("sessionId").asText())));
        assertEquals(1, events.get(1).path("turnNumber").asInt());
        assertEquals("mvn test", events.get(2).path("data").path("arguments").path("command").asText());
        assertEquals("completed", events.getLast().path("data").path("status").asText());
    }

    @Test
    void warnsAndContinuesWhenLogDirectoryCannotBeCreated() throws IOException {
        Path workspaceFile = tempDir.resolve("workspace-file");
        Files.writeString(workspaceFile, "不是目录", StandardCharsets.UTF_8);
        ByteArrayOutputStream warnings = new ByteArrayOutputStream();
        SessionLog sessionLog = new SessionLog(workspaceFile, new PrintStream(warnings, true, StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> sessionLog.start("任务"));
        assertTrue(warnings.toString(StandardCharsets.UTF_8).contains("会话日志写入失败"));
    }

    private JsonNode readTree(String line) {
        try {
            return objectMapper.readTree(line);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private Path logPath(Session session) {
        return tempDir.resolve(".minicodex/sessions").resolve(session.id() + ".jsonl");
    }
}
