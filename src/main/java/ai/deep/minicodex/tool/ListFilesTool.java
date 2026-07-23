package ai.deep.minicodex.tool;

import ai.deep.minicodex.model.ToolCall;
import ai.deep.minicodex.safety.WorkspacePolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListFilesTool implements Tool {
    private final WorkspacePolicy workspacePolicy;

    public ListFilesTool(WorkspacePolicy workspacePolicy) {
        this.workspacePolicy = workspacePolicy;
    }

    @Override
    public String name() {
        return "list_files";
    }

    @Override
    public String description() {
        return "列出工作区内某个目录的直接子文件。";
    }

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

    private String formatPath(Path path) {
        String type = Files.isDirectory(path) ? "[目录]" : "[文件]";
        return type + " " + workspacePolicy.workspaceRoot().relativize(path);
    }
}
