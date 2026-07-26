package ai.deep.minicodex.tool.file;

import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.safety.WorkspacePolicy;
import ai.deep.minicodex.tool.api.ToolResult;
import ai.deep.minicodex.tool.api.ToolSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteFileToolTest {
    @TempDir
    Path tempDir;

    @Test
    void createsNewFileInsideWorkspace() throws IOException {
        WriteFileTool tool = tool();

        ToolResult result = tool.execute(call("docs/demo.txt", "你好"));

        assertTrue(result.success());
        assertEquals("你好", Files.readString(tempDir.resolve("docs/demo.txt"), StandardCharsets.UTF_8));
        assertTrue(result.content().contains("已写入文件: docs"));
        assertTrue(result.content().contains("demo.txt"));
        assertTrue(result.content().contains("写入字符数: 2"));
    }

    @Test
    void createsParentDirectories() {
        WriteFileTool tool = tool();

        ToolResult result = tool.execute(call("notes/today/todo.txt", "任务"));

        assertTrue(result.success());
        assertTrue(Files.isRegularFile(tempDir.resolve("notes/today/todo.txt")));
    }

    @Test
    void rejectsExistingFile() throws IOException {
        Path existing = tempDir.resolve("README.md");
        Files.writeString(existing, "原内容", StandardCharsets.UTF_8);
        WriteFileTool tool = tool();

        ToolResult result = tool.execute(call("README.md", "新内容"));

        assertFalse(result.success());
        assertEquals("原内容", Files.readString(existing, StandardCharsets.UTF_8));
        assertTrue(result.content().contains("拒绝覆盖"));
    }

    @Test
    void rejectsPathOutsideWorkspace() {
        WriteFileTool tool = tool();

        ToolResult result = tool.execute(call("../outside.txt", "内容"));

        assertFalse(result.success());
        assertTrue(result.content().contains("路径越过了工作区边界"));
    }

    @Test
    void rejectsMissingPath() {
        WriteFileTool tool = tool();

        ToolResult result = tool.execute(new ToolCall("write_file", Map.of(
                "content", "内容"
        )));

        assertFalse(result.success());
        assertTrue(result.content().contains("缺少 path 参数"));
    }

    @Test
    void rejectsBlankPath() {
        WriteFileTool tool = tool();

        ToolResult result = tool.execute(call("  ", "内容"));

        assertFalse(result.success());
        assertTrue(result.content().contains("缺少 path 参数"));
    }

    @Test
    void rejectsMissingContent() {
        WriteFileTool tool = tool();

        ToolResult result = tool.execute(new ToolCall("write_file", Map.of(
                "path", "empty.txt"
        )));

        assertFalse(result.success());
        assertTrue(result.content().contains("缺少 content 参数"));
    }

    @Test
    void allowsEmptyContent() throws IOException {
        WriteFileTool tool = tool();

        ToolResult result = tool.execute(call("empty.txt", ""));

        assertTrue(result.success());
        assertEquals("", Files.readString(tempDir.resolve("empty.txt"), StandardCharsets.UTF_8));
        assertTrue(result.content().contains("写入字符数: 0"));
    }

    @Test
    void providesSchemaForPathAndContent() {
        WriteFileTool tool = tool();

        ToolSchema schema = tool.schema();

        assertEquals("write_file", schema.name());
        assertTrue(schema.parameters().containsKey("path"));
        assertTrue(schema.parameters().containsKey("content"));
    }

    private WriteFileTool tool() {
        return new WriteFileTool(new WorkspacePolicy(tempDir));
    }

    private ToolCall call(String path, String content) {
        return new ToolCall("write_file", Map.of(
                "path", path,
                "content", content
        ));
    }
}
