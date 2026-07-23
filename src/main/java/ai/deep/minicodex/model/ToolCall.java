package ai.deep.minicodex.model;

import java.util.Map;

public record ToolCall(String name, Map<String, String> arguments) {
    public String argument(String key) {
        return arguments.get(key);
    }
}
