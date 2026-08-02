package ai.deep.minicodex.model.context;

import ai.deep.minicodex.agent.ToolObservation;
import ai.deep.minicodex.model.api.ModelContext;
import ai.deep.minicodex.tool.api.ToolSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void doesNotCompactWhenObservationCountEqualsThreshold() {
        ContextBuilder builder = new ContextBuilder(List.of());
        List<ToolObservation> observations = List.of(
                new ToolObservation("first_tool", Map.of(), true, "第一条结果"),
                new ToolObservation("second_tool", Map.of(), true, "第二条结果"),
                new ToolObservation("third_tool", Map.of(), true, "第三条结果")
        );

        ModelContext context = builder.build("用户任务", observations);

        assertTrue(context.userContent().contains("历史工具观察结果:"));
        assertFalse(context.userContent().contains("已压缩的历史观察:"));
    }

    @Test
    void compactsOlderObservationsAndKeepsRecentObservationsComplete() {
        ContextBuilder builder = new ContextBuilder(List.of());
        ToolObservation first = new ToolObservation("first_tool", Map.of("path", "first.txt"), true, "第一条结果");
        ToolObservation second = new ToolObservation("second_tool", Map.of("path", "second.txt"), false, "第二条结果");
        ToolObservation third = new ToolObservation("third_tool", Map.of("path", "third.txt"), true, "第三条结果");
        ToolObservation fourth = new ToolObservation("fourth_tool", Map.of("path", "fourth.txt"), true, "第四条结果");

        ModelContext context = builder.build("保留这个用户目标", List.of(first, second, third, fourth));

        assertTrue(context.userContent().contains("保留这个用户目标"));
        assertTrue(context.userContent().contains("已压缩的历史观察:"));
        assertTrue(context.userContent().contains("最近工具观察:"));
        assertTrue(context.userContent().contains("工具: first_tool\n成功: true\n结果: 第一条结果"));
        assertFalse(context.userContent().contains("参数: {path=first.txt}"));
        assertTrue(context.userContent().contains("工具: third_tool\n参数: {path=third.txt}\n成功: true"));
        assertTrue(context.userContent().contains("工具: fourth_tool\n参数: {path=fourth.txt}\n成功: true"));
    }

    @Test
    void limitsCompactedHistoryLengthAndMarksOmittedObservations() {
        ContextBuilder builder = new ContextBuilder(List.of());
        List<ToolObservation> observations = List.of(
                observation("first_tool"),
                observation("second_tool"),
                observation("third_tool"),
                observation("fourth_tool"),
                observation("fifth_tool"),
                observation("sixth_tool"),
                observation("seventh_tool"),
                observation("eighth_tool")
        );

        ModelContext context = builder.build("长任务", observations);
        String compactedHistory = compactedHistory(context.userContent());

        assertTrue(compactedHistory.length() <= 1200);
        assertTrue(compactedHistory.contains("其余"));
        assertTrue(context.userContent().contains("工具: seventh_tool\n参数: {path=seventh_tool.txt}"));
        assertTrue(context.userContent().contains("工具: eighth_tool\n参数: {path=eighth_tool.txt}"));
    }

    private ToolObservation observation(String toolName) {
        return new ToolObservation(
                toolName,
                Map.of("path", toolName + ".txt"),
                true,
                "x".repeat(500)
        );
    }

    private String compactedHistory(String userContent) {
        int start = userContent.indexOf("已压缩的历史观察:\n") + "已压缩的历史观察:\n".length();
        int end = userContent.indexOf("\n---\n最近工具观察:");
        return userContent.substring(start, end);
    }
}
