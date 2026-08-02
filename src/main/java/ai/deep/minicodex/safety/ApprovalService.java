package ai.deep.minicodex.safety;

import ai.deep.minicodex.model.api.ToolCall;

import java.util.Objects;

/**
 * 根据工具名称决定是否需要用户审批。
 *
 * <p>审批策略集中在这里，具体工具只负责执行。当前阶段采用最小固定策略：
 * 读取工具直接允许，写入和命令工具需要询问，未知工具直接拒绝。</p>
 */
public class ApprovalService {
    private final ApprovalPrompt approvalPrompt;

    /**
     * 创建带用户询问器的审批服务。
     *
     * @param approvalPrompt 处理需要用户确认的调用
     */
    public ApprovalService(ApprovalPrompt approvalPrompt) {
        this.approvalPrompt = Objects.requireNonNull(approvalPrompt);
    }

    /**
     * 返回工具调用最终是否允许执行。
     *
     * @param toolCall 模型请求的工具调用
     * @return 允许执行时返回 {@code true}
     */
    public boolean approve(ToolCall toolCall) {
        return switch (decide(toolCall)) {
            case ALLOW -> true;
            case DENY -> false;
            case ASK -> approvalPrompt.ask(toolCall);
        };
    }

    /**
     * 返回当前审批策略的可读说明。
     *
     * @return 审批策略说明
     */
    public String describePolicy() {
        return "read_file、list_files：直接允许" + System.lineSeparator()
                + "write_file、run_command：需要确认" + System.lineSeparator()
                + "其他工具：拒绝";
    }

    private ApprovalDecision decide(ToolCall toolCall) {
        return switch (toolCall.name()) {
            case "read_file", "list_files" -> ApprovalDecision.ALLOW;
            case "write_file", "run_command" -> ApprovalDecision.ASK;
            default -> ApprovalDecision.DENY;
        };
    }
}
