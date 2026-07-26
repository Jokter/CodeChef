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
import java.util.Map;

/**
 * 读取工作区内文本文件的工具。
 *
 * <p>工具名为 {@code read_file}。它要求模型提供 {@code path} 参数，并始终通过
 * {@link WorkspacePolicy} 校验路径边界。文件内容按 UTF-8 文本读取。</p>
 */
public class ReadFileTool implements Tool {
    private final WorkspacePolicy workspacePolicy;

    /**
     * 创建读文件工具。
     *
     * @param workspacePolicy 工作区路径安全策略
     */
    public ReadFileTool(WorkspacePolicy workspacePolicy) {
        this.workspacePolicy = workspacePolicy;
    }

    /**
     * 返回模型调用该工具时使用的名称。
     *
     * @return 固定值 {@code read_file}
     */
    @Override
    public String name() {
        return "read_file";
    }

    /**
     * 返回工具能力说明。
     *
     * @return 工具描述文本
     */
    @Override
    public String description() {
        return "读取工作区内的文本文件。";
    }

    /**
     * 返回读文件工具的模型可见说明。
     *
     * @return 工具 schema
     */
    @Override
    public ToolSchema schema() {
        return new ToolSchema(
                name(),
                description(),
                Map.of("path", "目标文件路径。必填，必须位于工作区内。")
        );
    }

    /**
     * 执行文本文件读取。
     *
     * @param toolCall 工具调用；必填参数 {@code path} 表示目标文件
     * @return 文件内容；参数缺失、目标不是普通文件或读取失败时返回错误结果
     */
    @Override
    public ToolResult execute(ToolCall toolCall) {
        String inputPath = toolCall.argument("path");
        if (inputPath == null || inputPath.isBlank()) {
            return ToolResult.error("缺少 path 参数。");
        }

        Path file = workspacePolicy.resolveInsideWorkspace(inputPath);
        if (!Files.isRegularFile(file)) {
            return ToolResult.error("不是普通文件: " + file);
        }

        try {
            return ToolResult.ok(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ToolResult.error("读取文件失败: " + e.getMessage());
        }
    }
}
