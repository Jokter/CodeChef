package ai.deep.minicodex.safety;

import ai.deep.minicodex.model.api.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalServiceTest {
    @Test
    void allowsReadWithoutPrompt() {
        AtomicInteger prompts = new AtomicInteger();
        ApprovalService service = new ApprovalService(toolCall -> {
            prompts.incrementAndGet();
            return false;
        });

        assertTrue(service.approve(call("read_file")));
        assertTrue(service.approve(call("list_files")));
        assertEquals(0, prompts.get());
    }

    @Test
    void asksPromptForWriteAndCommandTools() {
        ApprovalService service = new ApprovalService(toolCall -> true);

        assertTrue(service.approve(call("write_file")));
        assertTrue(service.approve(call("run_command")));
    }

    @Test
    void rejectsWriteWhenPromptRejects() {
        ApprovalService service = new ApprovalService(toolCall -> false);

        assertFalse(service.approve(call("write_file")));
    }

    @Test
    void deniesUnknownWithoutPrompt() {
        AtomicInteger prompts = new AtomicInteger();
        ApprovalService service = new ApprovalService(toolCall -> {
            prompts.incrementAndGet();
            return true;
        });

        assertFalse(service.approve(call("unknown")));
        assertEquals(0, prompts.get());
    }

    private ToolCall call(String name) {
        return new ToolCall(name, Map.of());
    }
}
