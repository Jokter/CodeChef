package ai.deep.minicodex.model.parser;

import ai.deep.minicodex.model.api.ModelResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelOutputParserTest {
    private final ModelOutputParser parser = new ModelOutputParser();

    @Test
    void parsesFinalAnswer() {
        ModelResponse response = parser.parse("{\"type\":\"final\",\"answer\":\"完成\"}");

        assertTrue(response.isFinalAnswer());
        assertEquals("完成", response.finalAnswer());
    }

    @Test
    void parsesToolCall() {
        ModelResponse response = parser.parse("""
                {"type":"tool_call","tool":"read_file","arguments":{"path":"README.md","limit":20}}
                """);

        assertFalse(response.isFinalAnswer());
        assertEquals("read_file", response.toolCall().name());
        assertEquals(Map.of("path", "README.md", "limit", "20"), response.toolCall().arguments());
    }

    @Test
    void parsesJsonWrappedInMarkdown() {
        ModelResponse response = parser.parse("""
                ```json
                {"type":"final","answer":"已处理"}
                ```
                """);

        assertTrue(response.isFinalAnswer());
        assertEquals("已处理", response.finalAnswer());
    }

    @Test
    void rejectsInvalidOutput() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("我已经处理好了"));

        assertTrue(error.getMessage().contains("合法 JSON"));
    }

    @Test
    void rejectsUnknownType() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{\"type\":\"thinking\",\"answer\":\"处理中\"}"));

        assertTrue(error.getMessage().contains("未知的模型输出类型"));
    }

    @Test
    void rejectsMissingFinalAnswer() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{\"type\":\"final\"}"));

        assertTrue(error.getMessage().contains("answer"));
    }

    @Test
    void rejectsMissingToolArguments() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{\"type\":\"tool_call\",\"tool\":\"read_file\"}"));

        assertTrue(error.getMessage().contains("arguments"));
    }
}
