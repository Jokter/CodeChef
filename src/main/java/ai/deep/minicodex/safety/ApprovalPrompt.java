package ai.deep.minicodex.safety;

import ai.deep.minicodex.model.api.ToolCall;

/**
 * 处理需要用户确认的工具调用。
 */
public interface ApprovalPrompt {
    /**
     * 询问用户是否允许工具调用。
     *
     * @param toolCall 待确认的工具调用
     * @return 用户允许时返回 {@code true}
     */
    boolean ask(ToolCall toolCall);
}
