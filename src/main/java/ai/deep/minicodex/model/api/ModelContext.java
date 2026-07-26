package ai.deep.minicodex.model.api;

/**
 * 一次模型请求所需的已渲染上下文。
 *
 * @param systemPrompt 系统提示词
 * @param userContent 用户消息内容
 */
public record ModelContext(
        String systemPrompt,
        String userContent
) {
}
