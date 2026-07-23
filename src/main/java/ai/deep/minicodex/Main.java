package ai.deep.minicodex;

import ai.deep.minicodex.agent.AgentLoop;
import ai.deep.minicodex.model.FakeModelClient;
import ai.deep.minicodex.safety.WorkspacePolicy;
import ai.deep.minicodex.tool.ListFilesTool;
import ai.deep.minicodex.tool.ReadFileTool;
import ai.deep.minicodex.tool.ToolRegistry;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {
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
