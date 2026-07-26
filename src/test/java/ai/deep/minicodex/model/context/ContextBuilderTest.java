package ai.deep.minicodex.model.context;

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

        assertEquals("看看当前项目", context.userTask());
        assertEquals(List.of(), context.observations());
        assertTrue(context.userContent().contains("用户任务:\n看看当前项目"));
        assertTrue(context.userContent().contains("当前还没有工具观察结果。"));
    }

    @Test
    void rendersHistoricalObservationsInOrder() {
        ContextBuilder builder = new ContextBuilder(List.of());

        ModelContext context = builder.build("继续分析", List.of("第一次观察", "第二次观察"));

        assertEquals(List.of("第一次观察", "第二次观察"), context.observations());
        assertTrue(context.userContent().contains("历史工具观察结果:"));
        assertTrue(context.userContent().contains("第一次观察\n---\n第二次观察"));
    }
}
