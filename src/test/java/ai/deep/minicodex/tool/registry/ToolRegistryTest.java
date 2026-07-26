package ai.deep.minicodex.tool.registry;

import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.tool.api.Tool;
import ai.deep.minicodex.tool.api.ToolResult;
import ai.deep.minicodex.tool.api.ToolSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {
    @Test
    void executesRegisteredTool() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new FixedTool("echo", ToolResult.ok("完成")));

        ToolResult result = registry.execute(new ToolCall("echo", Map.of()));

        assertTrue(result.success());
        assertEquals("完成", result.content());
    }

    @Test
    void returnsSchemasInRegistrationOrder() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new FixedTool("first", ToolResult.ok("一")));
        registry.register(new FixedTool("second", ToolResult.ok("二")));

        List<ToolSchema> schemas = registry.schemas();

        assertEquals(List.of("first", "second"), schemas.stream().map(ToolSchema::name).toList());
    }

    @Test
    void returnsErrorForUnknownTool() {
        ToolRegistry registry = new ToolRegistry();

        ToolResult result = registry.execute(new ToolCall("missing", Map.of()));

        assertFalse(result.success());
        assertTrue(result.content().contains("未知工具: missing"));
    }

    @Test
    void convertsToolExceptionToError() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ThrowingTool());

        ToolResult result = registry.execute(new ToolCall("broken", Map.of()));

        assertFalse(result.success());
        assertTrue(result.content().contains("工具执行失败: boom"));
    }

    private record FixedTool(String name, ToolResult result) implements Tool {
        @Override
        public String description() {
            return "固定返回结果的测试工具。";
        }

        @Override
        public ToolSchema schema() {
            return new ToolSchema(name(), description(), Map.of());
        }

        @Override
        public ToolResult execute(ToolCall toolCall) {
            return result;
        }
    }

    private static class ThrowingTool implements Tool {
        @Override
        public String name() {
            return "broken";
        }

        @Override
        public String description() {
            return "抛出异常的测试工具。";
        }

        @Override
        public ToolSchema schema() {
            return new ToolSchema(name(), description(), Map.of());
        }

        @Override
        public ToolResult execute(ToolCall toolCall) {
            throw new IllegalStateException("boom");
        }
    }
}
