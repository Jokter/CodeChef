package ai.deep.minicodex.model;

/**
 * 模型响应。
 *
 * <p>一个响应只表示两种状态之一：最终回答或工具调用。为了保持结构简单，
 * 使用 {@code finalAnswer != null} 判断是否已完成任务。</p>
 *
 * @param finalAnswer 最终回答文本；非空时表示任务完成
 * @param toolCall 工具调用；当 {@code finalAnswer} 为空时使用
 */
public record ModelResponse(String finalAnswer, ToolCall toolCall) {
    /**
     * 创建最终回答响应。
     *
     * @param finalAnswer 最终回答文本
     * @return 最终回答响应
     */
    public static ModelResponse finalAnswer(String finalAnswer) {
        return new ModelResponse(finalAnswer, null);
    }

    /**
     * 创建工具调用响应。
     *
     * @param toolCall 模型请求执行的工具调用
     * @return 工具调用响应
     */
    public static ModelResponse toolCall(ToolCall toolCall) {
        return new ModelResponse(null, toolCall);
    }

    /**
     * 判断当前响应是否为最终回答。
     *
     * @return 如果包含最终回答则为 {@code true}
     */
    public boolean isFinalAnswer() {
        return finalAnswer != null;
    }
}
