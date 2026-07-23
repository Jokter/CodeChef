package ai.deep.minicodex.tool;

/**
 * 工具执行结果。
 *
 * @param success 工具是否执行成功
 * @param content 成功结果文本或错误说明
 */
public record ToolResult(boolean success, String content) {
    /**
     * 创建成功结果。
     *
     * @param content 工具返回内容
     * @return 成功的工具结果
     */
    public static ToolResult ok(String content) {
        return new ToolResult(true, content);
    }

    /**
     * 创建失败结果。
     *
     * @param content 错误说明
     * @return 失败的工具结果
     */
    public static ToolResult error(String content) {
        return new ToolResult(false, content);
    }
}
