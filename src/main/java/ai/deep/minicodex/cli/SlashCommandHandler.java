package ai.deep.minicodex.cli;

import ai.deep.minicodex.model.config.GptModelConfig;
import ai.deep.minicodex.safety.ApprovalService;
import ai.deep.minicodex.tool.api.ToolSchemaRenderer;
import ai.deep.minicodex.tool.registry.ToolRegistry;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 处理 CLI 的内部控制命令。
 */
public class SlashCommandHandler {
    private static final String AVAILABLE_COMMANDS = "/status, /tools, /permissions, /exit";

    private final GptModelConfig modelConfig;
    private final Path workspaceRoot;
    private final ToolRegistry toolRegistry;
    private final ApprovalService approvalService;
    private final int maxSteps;

    /**
     * 创建控制命令处理器。
     *
     * @param modelConfig 当前模型配置
     * @param workspaceRoot 当前工作区根目录
     * @param toolRegistry 当前工具注册表
     * @param approvalService 当前审批策略
     * @param maxSteps Agent 最大循环次数
     */
    public SlashCommandHandler(
            GptModelConfig modelConfig,
            Path workspaceRoot,
            ToolRegistry toolRegistry,
            ApprovalService approvalService,
            int maxSteps
    ) {
        this.modelConfig = Objects.requireNonNull(modelConfig);
        this.workspaceRoot = Objects.requireNonNull(workspaceRoot).toAbsolutePath().normalize();
        this.toolRegistry = Objects.requireNonNull(toolRegistry);
        this.approvalService = Objects.requireNonNull(approvalService);
        this.maxSteps = maxSteps;
    }

    /**
     * 处理一行 REPL 输入。
     *
     * @param input 原始输入
     * @return 命令处理结果
     */
    public CommandResult handle(String input) {
        String command = input.trim();
        if (command.isEmpty()) {
            return CommandResult.continueWithoutOutput();
        }
        if (!command.startsWith("/")) {
            return CommandResult.notACommand();
        }

        return switch (command) {
            case "/status" -> CommandResult.continueWith(status());
            case "/tools" -> CommandResult.continueWith(ToolSchemaRenderer.render(toolRegistry.schemas()));
            case "/permissions" -> CommandResult.continueWith(approvalService.describePolicy());
            case "/exit" -> CommandResult.exit("已退出。");
            default -> CommandResult.continueWith("未知控制命令: " + command
                    + System.lineSeparator()
                    + "可用命令: " + AVAILABLE_COMMANDS);
        };
    }

    private String status() {
        return "当前模型: " + modelConfig.name() + System.lineSeparator()
                + "工作区: " + workspaceRoot + System.lineSeparator()
                + "工具数量: " + toolRegistry.schemas().size() + System.lineSeparator()
                + "最大轮数: " + maxSteps;
    }

    /**
     * 控制命令处理结果。
     *
     * @param action 调度动作
     * @param output 要输出的文本；为空时不输出
     */
    public record CommandResult(Action action, String output) {
        private static CommandResult notACommand() {
            return new CommandResult(Action.NOT_A_COMMAND, "");
        }

        private static CommandResult continueWithoutOutput() {
            return new CommandResult(Action.CONTINUE, "");
        }

        private static CommandResult continueWith(String output) {
            return new CommandResult(Action.CONTINUE, output);
        }

        private static CommandResult exit(String output) {
            return new CommandResult(Action.EXIT, output);
        }
    }

    /**
     * Main 根据该动作决定下一步调度。
     */
    public enum Action {
        NOT_A_COMMAND,
        CONTINUE,
        EXIT
    }
}
