package ai.deep.minicodex.safety;

/**
 * 工具调用的审批决策。
 */
public enum ApprovalDecision {
    /** 直接允许执行。 */
    ALLOW,
    /** 直接拒绝执行。 */
    DENY,
    /** 需要询问用户后再决定。 */
    ASK
}
