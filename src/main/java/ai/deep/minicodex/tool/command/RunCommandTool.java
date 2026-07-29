package ai.deep.minicodex.tool.command;

import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.safety.WorkspacePolicy;
import ai.deep.minicodex.tool.api.Tool;
import ai.deep.minicodex.tool.api.ToolResult;
import ai.deep.minicodex.tool.api.ToolSchema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 在工作区内执行少量白名单开发命令的工具。
 *
 * <p>该工具不经过 shell 执行命令，因此不支持管道、重定向和命令串联。当前仅支持
 * Windows 环境，并固定在工作区根目录执行。</p>
 */
public class RunCommandTool implements Tool {
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int MAX_TIMEOUT_SECONDS = 120;
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;
    private static final int TERMINATION_WAIT_SECONDS = 2;

    private final WorkspacePolicy workspacePolicy;

    /**
     * 创建命令执行工具。
     *
     * @param workspacePolicy 工作区路径安全策略
     */
    public RunCommandTool(WorkspacePolicy workspacePolicy) {
        this.workspacePolicy = Objects.requireNonNull(workspacePolicy);
    }

    @Override
    public String name() {
        return "run_command";
    }

    @Override
    public String description() {
        return "在工作区内执行白名单中的基础开发命令。";
    }

    @Override
    public ToolSchema schema() {
        return new ToolSchema(
                name(),
                description(),
                Map.of(
                        "command", "必填。仅允许 mvn test、mvn compile、git status、git diff、"
                                + "rg <搜索表达式> [工作区内相对路径] 或 rg --files。",
                        "timeoutSeconds", "可选。命令超时秒数，默认 30，取值范围为 1 到 120。"
                )
        );
    }

    @Override
    public ToolResult execute(ToolCall toolCall) {
        String inputCommand = toolCall.argument("command");
        if (inputCommand == null || inputCommand.isBlank()) {
            return ToolResult.error("缺少 command 参数。");
        }

        int timeoutSeconds;
        try {
            timeoutSeconds = parseTimeoutSeconds(toolCall.argument("timeoutSeconds"));
        } catch (IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }

        List<String> command;
        try {
            command = parseCommand(inputCommand);
        } catch (IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }

        Process process;
        try {
            process = startProcess(command, workspacePolicy.workspaceRoot());
        } catch (IOException e) {
            return ToolResult.error("命令启动失败: " + e.getMessage());
        }

        OutputCollector outputCollector = new OutputCollector();
        ExecutorService outputReader = Executors.newFixedThreadPool(2);
        try {
            Future<?> stdoutFuture = outputReader.submit(() -> copyOutputSafely(
                    process.getInputStream(), outputCollector, OutputStreamType.STDOUT));
            Future<?> stderrFuture = outputReader.submit(() -> copyOutputSafely(
                    process.getErrorStream(), outputCollector, OutputStreamType.STDERR));

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                terminate(process);
                waitForOutput(stdoutFuture, stderrFuture);
                return ToolResult.error(renderResult(
                        command,
                        "命令执行超时，已终止。",
                        processExitCode(process),
                        outputCollector
                ));
            }

            waitForOutput(stdoutFuture, stderrFuture);
            int exitCode = process.exitValue();
            String result = renderResult(command, null, Integer.toString(exitCode), outputCollector);
            return exitCode == 0 ? ToolResult.ok(result) : ToolResult.error(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            terminate(process);
            return ToolResult.error("命令执行被中断。");
        } catch (ExecutionException e) {
            terminate(process);
            return ToolResult.error("读取命令输出失败: " + e.getCause().getMessage());
        } finally {
            outputReader.shutdownNow();
        }
    }

    Process startProcess(List<String> command, Path workingDirectory) throws IOException {
        return new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .start();
    }

    private int parseTimeoutSeconds(String timeoutSeconds) {
        if (timeoutSeconds == null) {
            return DEFAULT_TIMEOUT_SECONDS;
        }

        try {
            int parsed = Integer.parseInt(timeoutSeconds);
            if (parsed < 1 || parsed > MAX_TIMEOUT_SECONDS) {
                throw new IllegalArgumentException("timeoutSeconds 必须在 1 到 120 之间。");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("timeoutSeconds 必须是整数。");
        }
    }

    private List<String> parseCommand(String inputCommand) {
        rejectShellSyntax(inputCommand);
        List<String> tokens = tokenize(inputCommand);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("command 参数不能为空。");
        }

        return switch (tokens.getFirst()) {
            case "mvn" -> parseMavenCommand(tokens);
            case "git" -> parseGitCommand(tokens);
            case "rg" -> parseRgCommand(tokens);
            default -> throw new IllegalArgumentException("不允许执行该命令: " + tokens.getFirst());
        };
    }

    private List<String> parseMavenCommand(List<String> tokens) {
        if (tokens.size() != 2 || !("test".equals(tokens.get(1)) || "compile".equals(tokens.get(1)))) {
            throw new IllegalArgumentException("仅允许执行: mvn test 或 mvn compile");
        }
        return List.of("mvn.cmd", tokens.get(1));
    }

    private List<String> parseGitCommand(List<String> tokens) {
        if (tokens.size() != 2 || !("status".equals(tokens.get(1)) || "diff".equals(tokens.get(1)))) {
            throw new IllegalArgumentException("仅允许执行: git status 或 git diff");
        }
        return List.of("git", tokens.get(1));
    }

    private List<String> parseRgCommand(List<String> tokens) {
        if (tokens.size() == 2 && "--files".equals(tokens.get(1))) {
            return List.of("rg", "--files");
        }
        if (tokens.size() < 2 || tokens.size() > 3 || tokens.get(1).startsWith("-")) {
            throw new IllegalArgumentException("rg 仅允许: rg <搜索表达式> [工作区内相对路径] 或 rg --files");
        }

        List<String> command = new ArrayList<>();
        command.add("rg");
        command.add(tokens.get(1));
        if (tokens.size() == 3) {
            Path path;
            try {
                path = workspacePolicy.resolveInsideWorkspace(tokens.get(2));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("rg 搜索路径无效: " + e.getMessage());
            }
            command.add(workspacePolicy.workspaceRoot().relativize(path).toString());
        }
        return command;
    }

    private void rejectShellSyntax(String inputCommand) {
        if (inputCommand.indexOf('&') >= 0 || inputCommand.indexOf('|') >= 0
                || inputCommand.indexOf(';') >= 0 || inputCommand.indexOf('>') >= 0
                || inputCommand.indexOf('<') >= 0 || inputCommand.indexOf('\n') >= 0
                || inputCommand.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("command 不允许包含 shell 特殊字符。 ");
        }
    }

    private List<String> tokenize(String inputCommand) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int index = 0; index < inputCommand.length(); index++) {
            char character = inputCommand.charAt(index);
            if (quote != 0) {
                if (character == quote) {
                    quote = 0;
                } else {
                    current.append(character);
                }
                continue;
            }
            if (character == '\'' || character == '"') {
                quote = character;
            } else if (Character.isWhitespace(character)) {
                addToken(tokens, current);
            } else {
                current.append(character);
            }
        }
        if (quote != 0) {
            throw new IllegalArgumentException("command 中存在未闭合的引号。");
        }
        addToken(tokens, current);
        return tokens;
    }

    private void addToken(List<String> tokens, StringBuilder current) {
        if (!current.isEmpty()) {
            tokens.add(current.toString());
            current.setLength(0);
        }
    }

    private void copyOutput(InputStream input, OutputCollector outputCollector, OutputStreamType streamType)
            throws IOException {
        try (input) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                outputCollector.append(streamType, buffer, read);
            }
        }
    }

    private void copyOutputSafely(InputStream input, OutputCollector outputCollector, OutputStreamType streamType) {
        try {
            copyOutput(input, outputCollector, streamType);
        } catch (IOException e) {
            throw new CommandOutputException(e);
        }
    }

    private void waitForOutput(Future<?> stdoutFuture, Future<?> stderrFuture)
            throws InterruptedException, ExecutionException {
        stdoutFuture.get();
        stderrFuture.get();
    }

    private void terminate(Process process) {
        process.destroy();
        try {
            if (!process.waitFor(TERMINATION_WAIT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(TERMINATION_WAIT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private String processExitCode(Process process) {
        try {
            return Integer.toString(process.exitValue());
        } catch (IllegalThreadStateException e) {
            return "未知";
        }
    }

    private String renderResult(
            List<String> command,
            String status,
            String exitCode,
            OutputCollector outputCollector
    ) {
        StringBuilder result = new StringBuilder();
        result.append("命令: ").append(String.join(" ", command)).append('\n');
        if (status != null) {
            result.append("状态: ").append(status).append('\n');
        }
        result.append("退出码: ").append(exitCode).append('\n');
        result.append("stdout:\n").append(outputCollector.content(OutputStreamType.STDOUT));
        if (outputCollector.truncated(OutputStreamType.STDOUT)) {
            result.append("\n[stdout 已截断]");
        }
        result.append("\nstderr:\n").append(outputCollector.content(OutputStreamType.STDERR));
        if (outputCollector.truncated(OutputStreamType.STDERR)) {
            result.append("\n[stderr 已截断]");
        }
        return result.toString();
    }

    private enum OutputStreamType {
        STDOUT,
        STDERR
    }

    private static class OutputCollector {
        private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        private int remainingBytes = MAX_OUTPUT_BYTES;
        private boolean stdoutTruncated;
        private boolean stderrTruncated;

        synchronized void append(OutputStreamType streamType, byte[] buffer, int length) {
            int acceptedLength = Math.min(length, remainingBytes);
            output(streamType).write(buffer, 0, acceptedLength);
            remainingBytes -= acceptedLength;
            if (acceptedLength < length) {
                markTruncated(streamType);
            }
        }

        synchronized String content(OutputStreamType streamType) {
            return output(streamType).toString(StandardCharsets.UTF_8);
        }

        synchronized boolean truncated(OutputStreamType streamType) {
            return streamType == OutputStreamType.STDOUT ? stdoutTruncated : stderrTruncated;
        }

        private ByteArrayOutputStream output(OutputStreamType streamType) {
            return streamType == OutputStreamType.STDOUT ? stdout : stderr;
        }

        private void markTruncated(OutputStreamType streamType) {
            if (streamType == OutputStreamType.STDOUT) {
                stdoutTruncated = true;
            } else {
                stderrTruncated = true;
            }
        }
    }

    private static class CommandOutputException extends RuntimeException {
        private CommandOutputException(IOException cause) {
            super(cause);
        }
    }
}
