package ai.deep.minicodex.safety;

import ai.deep.minicodex.model.api.ToolCall;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleApprovalPromptTest {
    @Test
    void acceptsYesAnswer() {
        ConsoleApprovalPrompt prompt = prompt("yes\n");

        assertTrue(prompt.ask(call()));
    }

    @Test
    void rejectsNoAnswer() {
        ConsoleApprovalPrompt prompt = prompt("n\n");

        assertFalse(prompt.ask(call()));
    }

    @Test
    void rejectsWhenInputEnds() {
        ConsoleApprovalPrompt prompt = prompt("");

        assertFalse(prompt.ask(call()));
    }

    private ConsoleApprovalPrompt prompt(String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        return new ConsoleApprovalPrompt(
                new Scanner(input),
                new PrintStream(output, true, StandardCharsets.UTF_8)
        );
    }

    private ToolCall call() {
        return new ToolCall("write_file", Map.of("path", "demo.txt"));
    }
}
