package ai.deep.minicodex.cli;

import ai.deep.minicodex.agent.AgentLoop;
import ai.deep.minicodex.agent.session.SessionLog;
import ai.deep.minicodex.model.client.GptModelClient;
import ai.deep.minicodex.model.config.GptModelConfig;
import ai.deep.minicodex.model.context.ContextBuilder;
import ai.deep.minicodex.safety.ApprovalService;
import ai.deep.minicodex.safety.ConsoleApprovalPrompt;
import ai.deep.minicodex.safety.WorkspacePolicy;
import ai.deep.minicodex.tool.command.RunCommandTool;
import ai.deep.minicodex.tool.file.ListFilesTool;
import ai.deep.minicodex.tool.file.ReadFileTool;
import ai.deep.minicodex.tool.file.WriteFileTool;
import ai.deep.minicodex.tool.registry.ToolRegistry;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * 应用程序入口。
 *
 * <p>该类负责完成最小 Agent 运行环境的装配：确定工作区根目录、创建路径安全策略、
 * 注册可用工具、创建模型客户端与 Agent 主循环，并把用户输入交给 Agent 执行。
 * 具体的模型决策、工具分发和文件访问逻辑分别由对应模块处理。</p>
 */
public class Main {
    /**
     * 启动命令行程序。
     *
     * @param args 用户任务参数；为空时从标准输入读取任务
     */
    public static void main(String[] args) throws Exception {
        Path workspaceRoot = Path.of("").toAbsolutePath().normalize();
        WorkspacePolicy workspacePolicy = new WorkspacePolicy(workspaceRoot);
        Scanner scanner = new Scanner(System.in);

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new ListFilesTool(workspacePolicy));
        toolRegistry.register(new ReadFileTool(workspacePolicy));
        toolRegistry.register(new WriteFileTool(workspacePolicy));
        toolRegistry.register(new RunCommandTool(workspacePolicy));

        GptModelConfig modelConfig = GptModelConfig.loadDefault();
        ContextBuilder contextBuilder = new ContextBuilder(toolRegistry.schemas());
        ApprovalService approvalService = new ApprovalService(new ConsoleApprovalPrompt(scanner, System.out));
        AgentLoop agentLoop = new AgentLoop(
                new GptModelClient(modelConfig),
                contextBuilder,
                toolRegistry,
                approvalService,
                new SessionLog(workspaceRoot)
        );
        SlashCommandHandler slashCommandHandler = new SlashCommandHandler(
                modelConfig,
                workspaceRoot,
                toolRegistry,
                approvalService,
                AgentLoop.maxSteps()
        );

        if (args.length == 0) {
            runRepl(scanner, workspaceRoot, agentLoop, slashCommandHandler);
            return;
        }

        runTask(String.join(" ", args), workspaceRoot, agentLoop);
    }

    /**
     * 运行交互式命令行循环。
     *
     * @param scanner 控制台输入
     * @param workspaceRoot 工作区根目录
     * @param agentLoop Agent 主循环
     * @param slashCommandHandler 控制命令处理器
     */
    private static void runRepl(
            Scanner scanner,
            Path workspaceRoot,
            AgentLoop agentLoop,
            SlashCommandHandler slashCommandHandler
    ) {
        System.out.println("进入交互模式，输入 /exit 退出。");
        while (true) {
            System.out.print("请输入任务或命令: ");
            if (!scanner.hasNextLine()) {
                System.out.println();
                System.out.println("输入结束，已退出。");
                return;
            }

            String input = scanner.nextLine();
            SlashCommandHandler.CommandResult result = slashCommandHandler.handle(input);
            if (result.action() == SlashCommandHandler.Action.NOT_A_COMMAND) {
                runTask(input.trim(), workspaceRoot, agentLoop);
                continue;
            }
            if (!result.output().isBlank()) {
                System.out.println(result.output());
            }
            if (result.action() == SlashCommandHandler.Action.EXIT) {
                return;
            }
        }
    }

    private static void runTask(String task, Path workspaceRoot, AgentLoop agentLoop) {
        System.out.println("工作区: " + workspaceRoot);
        System.out.println("任务: " + task);
        System.out.println();

        String answer = agentLoop.run(task);
        System.out.println();
        System.out.println("最终回答:");
        System.out.println(answer);
    }
}
