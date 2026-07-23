package ai.deep.minicodex.tool;

import ai.deep.minicodex.model.ToolCall;
import ai.deep.minicodex.safety.WorkspacePolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReadFileTool implements Tool {
    private final WorkspacePolicy workspacePolicy;

    public ReadFileTool(WorkspacePolicy workspacePolicy) {
        this.workspacePolicy = workspacePolicy;
    }

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String description() {
        return "读取工作区内的文本文件。";
    }

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
