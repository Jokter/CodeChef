package ai.deep.minicodex.agent.session;

/**
 * 会话中的一次模型决策轮次。
 *
 * @param number 从 1 开始的轮次编号
 */
public record Turn(int number) {
    public Turn {
        if (number < 1) {
            throw new IllegalArgumentException("轮次编号必须大于 0。");
        }
    }
}
