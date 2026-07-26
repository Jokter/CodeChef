package ai.deep.minicodex.model.api;

import java.util.List;

/**
 * 一次模型请求所需的上下文。
 *
 * @param userTask 用户原始任务
 * @param observations 历史工具观察结果，按产生顺序排列
 * @param systemPrompt 系统提示词
 * @param userContent 用户消息内容
 */
public record ModelContext(
        String userTask,
        List<String> observations,
        String systemPrompt,
        String userContent
) {
    public ModelContext {
        observations = List.copyOf(observations);
    }
}
