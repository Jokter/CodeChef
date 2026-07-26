package ai.deep.minicodex.tool.file;

import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.safety.WorkspacePolicy;
import ai.deep.minicodex.tool.api.Tool;
import ai.deep.minicodex.tool.api.ToolResult;
import ai.deep.minicodex.tool.api.ToolSchema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * 在工作区内创建新文本文件的工具。
 *
 * <p>工具名为 {@code write_file}。它只允许创建不存在的新文件，不覆盖已有文件。
 * 写入前会通过 {@link WorkspacePolicy} 校验最终目标路径。</p>
 */
public class WriteFileTool implements Tool {
    private final WorkspacePolicy workspacePolicy;

    /**
     * 创建写文件工具。
     *
     * @param workspacePolicy 工作区路径安全策略
     */
    public WriteFileTool(WorkspacePolicy workspacePolicy) {
        this.workspacePolicy = workspacePolicy;
    }

    /**
     * 返回模型调用该工具时使用的名称。
     *
     * @return 固定值 {@code write_file}
     */
    @Override
    public String name() {
        return "write_file";
    }

    /**
     * 返回工具能力说明。
     *
     * @return 工具描述文本
     */
    @Override
    public String description() {
        return "在工作区内创建新的文本文件。";
    }

    /**
     * 返回写文件工具的模型可见说明。
     *
     * @return 工具 schema
     */
    @Override
    public ToolSchema schema() {
        return new ToolSchema(
                name(),
                description(),
                Map.of(
                        "path", "目标文件路径。必填，必须位于工作区内，且目标文件不能已存在。",
                        "content", "要写入的完整文件内容。必填，可以为空字符串。"
                )
        );
    }

    /**
     * 执行文本文件创建。
     *
     * @param toolCall 工具调用；必填参数 {@code path} 和 {@code content}
     * @return 写入确认；参数缺失、路径越界、目标已存在或写入失败时返回错误结果
     */
    @Override
    public ToolResult execute(ToolCall toolCall) {
        String inputPath = toolCall.argument("path");
        if (inputPath == null || inputPath.isBlank()) {
            return ToolResult.error("缺少 path 参数。");
        }

        String content = toolCall.argument("content");
        if (content == null) {
            return ToolResult.error("缺少 content 参数。");
        }

        Path file;
        try {
            file = workspacePolicy.resolveInsideWorkspace(inputPath);
        } catch (IllegalArgumentException e) {
            return ToolResult.error(e.getMessage());
        }

        if (Files.exists(file)) {
            return ToolResult.error("目标文件已存在，拒绝覆盖: " + file);
        }

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            return ToolResult.ok("""
                    已写入文件: %s
                    写入字符数: %d
                    """.formatted(workspacePolicy.workspaceRoot().relativize(file), content.length()));
        } catch (IOException e) {
            return ToolResult.error("写入文件失败: " + e.getMessage());
        }
    }
}
