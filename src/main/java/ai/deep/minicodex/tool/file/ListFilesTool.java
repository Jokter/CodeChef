package ai.deep.minicodex.tool.file;

import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.safety.WorkspacePolicy;
import ai.deep.minicodex.tool.api.Tool;
import ai.deep.minicodex.tool.api.ToolResult;
import ai.deep.minicodex.tool.api.ToolSchema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 列出工作区内目录直接子项的工具。
 *
 * <p>工具名为 {@code list_files}。它读取参数 {@code path} 指定的目录；
 * 参数为空时默认使用工作区根目录。该工具只列出直接子项，不递归遍历子目录。</p>
 */
public class ListFilesTool implements Tool {
    private final WorkspacePolicy workspacePolicy;

    /**
     * 创建列目录工具。
     *
     * @param workspacePolicy 工作区路径安全策略
     */
    public ListFilesTool(WorkspacePolicy workspacePolicy) {
        this.workspacePolicy = workspacePolicy;
    }

    /**
     * 返回模型调用该工具时使用的名称。
     *
     * @return 固定值 {@code list_files}
     */
    @Override
    public String name() {
        return "list_files";
    }

    /**
     * 返回工具能力说明。
     *
     * @return 工具描述文本
     */
    @Override
    public String description() {
        return "列出工作区内某个目录的直接子文件。";
    }

    /**
     * 返回列目录工具的模型可见说明。
     *
     * @return 工具 schema
     */
    @Override
    public ToolSchema schema() {
        return new ToolSchema(
                name(),
                description(),
                Map.of("path", "目标目录路径。可选，缺省为工作区根目录。")
        );
    }

    /**
     * 执行列目录操作。
     *
     * @param toolCall 工具调用；可选参数 {@code path} 表示目标目录
     * @return 目录子项列表；目标不是目录或读取失败时返回错误结果
     */
    @Override
    public ToolResult execute(ToolCall toolCall) {
        String inputPath = toolCall.argument("path");
        Path dir = workspacePolicy.resolveInsideWorkspace(inputPath == null ? "." : inputPath);

        if (!Files.isDirectory(dir)) {
            return ToolResult.error("不是目录: " + dir);
        }

        try (Stream<Path> stream = Files.list(dir)) {
            String content = stream
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(this::formatPath)
                    .collect(Collectors.joining(System.lineSeparator()));

            return ToolResult.ok(content.isBlank() ? "目录为空。" : content);
        } catch (IOException e) {
            return ToolResult.error("读取目录失败: " + e.getMessage());
        }
    }

    /**
     * 将文件系统路径格式化为面向用户的相对路径展示。
     *
     * @param path 工作区内的文件或目录路径
     * @return 带有类型标记的相对路径，例如 {@code [文件] README.md}
     */
    private String formatPath(Path path) {
        String type = Files.isDirectory(path) ? "[目录]" : "[文件]";
        return type + " " + workspacePolicy.workspaceRoot().relativize(path);
    }
}
