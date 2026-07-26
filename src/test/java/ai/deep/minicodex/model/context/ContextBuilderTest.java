package ai.deep.minicodex.model.context;

import ai.deep.minicodex.agent.ToolObservation;
import ai.deep.minicodex.model.api.ModelContext;
import ai.deep.minicodex.tool.api.ToolSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextBuilderTest {
    @Test
    void rendersToolSchemasIntoSystemPrompt() {
        ContextBuilder builder = new ContextBuilder(List.of(
                new ToolSchema("read_file", "读取工作区内文件。", Map.of(
                        "path", "相对工作区的文件路径。"
                ))
        ));

        ModelContext context = builder.build("读取 README", List.of());

        assertTrue(context.systemPrompt().contains("- read_file: 读取工作区内文件。"));
        assertTrue(context.systemPrompt().contains("  - path: 相对工作区的文件路径。"));
        assertTrue(context.systemPrompt().contains("{\"type\":\"tool_call\""));
    }

    @Test
    void rendersEmptyToolListClearly() {
        ContextBuilder builder = new ContextBuilder(List.of());

        ModelContext context = builder.build("你好", List.of());

        assertTrue(context.systemPrompt().contains("当前没有可用工具。"));
    }

    @Test
    void rendersUserTaskWithoutObservations() {
        ContextBuilder builder = new ContextBuilder(List.of());

        ModelContext context = builder.build("看看当前项目", List.of());

        assertTrue(context.userContent().contains("【用户任务开始】\n看看当前项目\n【用户任务结束】"));
        assertTrue(context.userContent().contains("【工具观察结果开始】"));
        assertTrue(context.userContent().contains("当前还没有工具观察结果。"));
        assertTrue(context.userContent().contains("【工具观察结果结束】"));
    }

    @Test
    void separatesTaskTextFromObservationNotice() {
        ContextBuilder builder = new ContextBuilder(List.of());

        ModelContext context = builder.build("请创建文件，内容是：", List.of());

        assertTrue(context.userContent().contains("内容是：\n【用户任务结束】"));
        assertTrue(context.userContent().contains("【工具观察结果开始】\n当前还没有工具观察结果。"));
    }

    @Test
    void rendersHistoricalObservationsInOrder() {
        ContextBuilder builder = new ContextBuilder(List.of());
        ToolObservation first = new ToolObservation("list_files", Map.of("path", "."), true, "README.md");
        ToolObservation second = new ToolObservation("read_file", Map.of("path", "README.md"), false, "读取失败");

        ModelContext context = builder.build("继续分析", List.of(first, second));

        assertTrue(context.userContent().contains("历史工具观察结果:"));
        assertTrue(context.userContent().contains("工具: list_files"));
        assertTrue(context.userContent().contains("参数: {path=.}"));
        assertTrue(context.userContent().contains("成功: true"));
        assertTrue(context.userContent().contains("结果:\nREADME.md"));
        assertTrue(context.userContent().contains("---"));
        assertTrue(context.userContent().contains("工具: read_file"));
        assertTrue(context.userContent().contains("参数: {path=README.md}"));
        assertTrue(context.userContent().contains("成功: false"));
        assertTrue(context.userContent().contains("结果:\n读取失败"));
    }
}
