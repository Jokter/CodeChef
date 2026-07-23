package ai.deep.minicodex.model;

import java.util.Map;

/**
 * 模型请求执行的工具调用。
 *
 * @param name 工具名称，必须与 {@code Tool.name()} 返回值一致
 * @param arguments 工具参数表；当前实现只支持字符串参数
 */
public record ToolCall(String name, Map<String, String> arguments) {
    /**
     * 读取指定名称的工具参数。
     *
     * @param key 参数名
     * @return 参数值；不存在时返回 {@code null}
     */
    public String argument(String key) {
        return arguments.get(key);
    }
}
