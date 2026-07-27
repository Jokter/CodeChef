package ai.deep.minicodex.safety;

import ai.deep.minicodex.model.api.ToolCall;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Scanner;

/**
 * 通过命令行向用户确认工具调用。
 */
public class ConsoleApprovalPrompt implements ApprovalPrompt {
    private final Scanner scanner;
    private final PrintStream output;

    public ConsoleApprovalPrompt(Scanner scanner, PrintStream output) {
        this.scanner = Objects.requireNonNull(scanner);
        this.output = Objects.requireNonNull(output);
    }

    @Override
    public boolean ask(ToolCall toolCall) {
        output.println("工具调用需要用户审批。");
        output.println("请求执行工具: " + toolCall.name());
        output.println("工具参数: " + toolCall.arguments());
        output.print("允许执行吗？[y/N] ");
        if (!scanner.hasNextLine()) {
            return false;
        }
        String answer = scanner.nextLine().trim();
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }
}
