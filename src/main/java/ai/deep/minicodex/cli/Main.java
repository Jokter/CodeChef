package ai.deep.minicodex.cli;

import ai.deep.minicodex.agent.AgentLoop;
import ai.deep.minicodex.model.client.FakeModelClient;
import ai.deep.minicodex.safety.WorkspacePolicy;
import ai.deep.minicodex.tool.file.ListFilesTool;
import ai.deep.minicodex.tool.file.ReadFileTool;
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
    public static void main(String[] args) {
        Path workspaceRoot = Path.of("").toAbsolutePath().normalize();
        WorkspacePolicy workspacePolicy = new WorkspacePolicy(workspaceRoot);

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new ListFilesTool(workspacePolicy));
        toolRegistry.register(new ReadFileTool(workspacePolicy));

        AgentLoop agentLoop = new AgentLoop(new FakeModelClient(), toolRegistry);

        String task = readTask(args);
        System.out.println("工作区: " + workspaceRoot);
        System.out.println("任务: " + task);
        System.out.println();

        String answer = agentLoop.run(task);
        System.out.println();
        System.out.println("最终回答:");
        System.out.println(answer);
    }

    /**
     * 从命令行参数或标准输入读取用户任务。
     *
     * <p>当命令行参数非空时，将所有参数用空格拼接为任务文本；否则提示用户输入。
     * 如果用户直接回车，则返回一个默认任务，方便本地快速演示。</p>
     *
     * @param args 命令行参数
     * @return 用户任务文本
     */
    private static String readTask(String[] args) {
        if (args.length > 0) {
            return String.join(" ", args);
        }

        System.out.print("请输入任务: ");
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine().trim();
        return line.isEmpty() ? "看看当前项目里有什么文件" : line;
    }
}
