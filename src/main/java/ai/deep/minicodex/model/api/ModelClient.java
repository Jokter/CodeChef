package ai.deep.minicodex.model.api;

/**
 * 模型客户端抽象。
 *
 * <p>Agent 只依赖该接口，不关心背后是真实大模型、假模型，还是其他规则引擎。
 * 实现类需要根据模型上下文决定下一步：返回最终回答或请求工具调用。</p>
 */
public interface ModelClient {
    /**
     * 生成下一步模型响应。
     *
     * @param context 模型请求上下文
     * @return 模型响应，可能是最终回答，也可能是工具调用
     */
    ModelResponse next(ModelContext context);
}
