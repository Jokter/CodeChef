package ai.deep.minicodex.agent.session;

import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.tool.api.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 将 Agent 执行事件追加写入本地 JSONL 文件。
 *
 * <p>日志写入失败只输出告警，不影响 Agent 的执行流程。</p>
 */
public class SessionLog {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Path sessionsDirectory;
    private final PrintStream warningOutput;

    /**
     * 创建工作区会话日志。
     *
     * @param workspaceRoot 工作区根目录
     */
    public SessionLog(Path workspaceRoot) {
        this(workspaceRoot, System.err);
    }

    SessionLog(Path workspaceRoot, PrintStream warningOutput) {
        this.sessionsDirectory = Objects.requireNonNull(workspaceRoot)
                .toAbsolutePath()
                .normalize()
                .resolve(".minicodex")
                .resolve("sessions");
        this.warningOutput = Objects.requireNonNull(warningOutput);
    }

    /**
     * 创建会话并记录用户输入。
     *
     * @param userTask 用户任务
     * @return 新创建的会话
     */
    public Session start(String userTask) {
        Session session = Session.create();
        append(session, null, "user_input", Map.of(
                "content", userTask,
                "sessionStartedAt", session.startedAt().toString()
        ));
        return session;
    }

    /**
     * 记录模型响应。
     *
     * @param session 当前会话
     * @param turn 当前轮次
     * @param response 模型响应
     */
    public void recordModelResponse(Session session, Turn turn, ModelResponse response) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (response.isFinalAnswer()) {
            data.put("responseType", "final");
            data.put("answer", response.finalAnswer());
        } else {
            ToolCall toolCall = response.toolCall();
            data.put("responseType", "tool_call");
            data.put("toolName", toolCall.name());
            data.put("arguments", toolCall.arguments());
        }
        append(session, turn, "model_response", data);
    }

    /**
     * 记录工具调用。
     *
     * @param session 当前会话
     * @param turn 当前轮次
     * @param toolCall 工具调用
     */
    public void recordToolCall(Session session, Turn turn, ToolCall toolCall) {
        append(session, turn, "tool_call", Map.of(
                "toolName", toolCall.name(),
                "arguments", toolCall.arguments()
        ));
    }

    /**
     * 记录工具执行结果。
     *
     * @param session 当前会话
     * @param turn 当前轮次
     * @param toolResult 工具执行结果
     */
    public void recordToolResult(Session session, Turn turn, ToolResult toolResult) {
        append(session, turn, "tool_result", Map.of(
                "success", toolResult.success(),
                "content", toolResult.content()
        ));
    }

    /**
     * 记录最终回答。
     *
     * @param session 当前会话
     * @param turn 当前轮次
     * @param answer 最终回答
     */
    public void recordFinalAnswer(Session session, Turn turn, String answer) {
        append(session, turn, "final_answer", Map.of("content", answer));
    }

    /**
     * 记录会话结束状态。
     *
     * @param session 当前会话
     * @param status 会话状态
     * @param failure 异常失败原因；正常结束时为 {@code null}
     */
    public void finish(Session session, String status, Throwable failure) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", status);
        if (failure != null) {
            data.put("errorType", failure.getClass().getName());
            if (failure.getMessage() != null) {
                data.put("errorMessage", failure.getMessage());
            }
        }
        append(session, null, "session_finished", data);
    }

    private void append(Session session, Turn turn, String type, Map<String, ?> data) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("sessionId", session.id());
        event.put("timestamp", Instant.now().toString());
        if (turn != null) {
            event.put("turnNumber", turn.number());
        }
        event.put("type", type);
        event.put("data", data);

        try {
            Files.createDirectories(sessionsDirectory);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    logPath(session),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                writer.write(OBJECT_MAPPER.writeValueAsString(event));
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            warningOutput.println("会话日志写入失败: " + e.getMessage());
        }
    }

    private Path logPath(Session session) {
        return sessionsDirectory.resolve(session.id() + ".jsonl");
    }
}
