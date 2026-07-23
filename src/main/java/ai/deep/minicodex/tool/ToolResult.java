package ai.deep.minicodex.tool;

public record ToolResult(boolean success, String content) {
    public static ToolResult ok(String content) {
        return new ToolResult(true, content);
    }

    public static ToolResult error(String content) {
        return new ToolResult(false, content);
    }
}
