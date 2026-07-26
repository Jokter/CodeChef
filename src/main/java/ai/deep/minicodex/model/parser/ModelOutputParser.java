package ai.deep.minicodex.model.parser;

import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 解析模型输出协议。
 *
 * <p>该类只处理模型返回给 agent 的业务协议，不处理 OpenAI 或 Anthropic 的原始响应外壳。</p>
 */
public class ModelOutputParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 解析模型返回的协议文本。
     *
     * @param content 模型输出文本
     * @return 模型响应
     */
    public ModelResponse parse(String content) {
        JsonNode root = readJsonObject(content);
        String type = requiredText(root, "type");

        if ("final".equals(type)) {
            return ModelResponse.finalAnswer(requiredText(root, "answer"));
        }

        if ("tool_call".equals(type)) {
            JsonNode arguments = requiredObject(root, "arguments");
            return ModelResponse.toolCall(new ToolCall(
                    requiredText(root, "tool"),
                    readStringArguments(arguments)
            ));
        }

        throw new IllegalArgumentException("未知的模型输出类型: " + type);
    }

    private JsonNode readJsonObject(String content) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(extractJsonObject(content));
            if (!root.isObject()) {
                throw new IllegalArgumentException("模型输出必须是 JSON 对象。");
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("模型输出不是合法 JSON 对象。", e);
        }
    }

    private String extractJsonObject(String content) {
        String trimmed = content.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String requiredText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isTextual()) {
            throw new IllegalArgumentException("模型输出缺少文本字段: " + fieldName);
        }
        return node.asText();
    }

    private JsonNode requiredObject(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("模型输出缺少对象字段: " + fieldName);
        }
        return node;
    }

    private Map<String, String> readStringArguments(JsonNode argumentsNode) {
        Map<String, String> arguments = new LinkedHashMap<>();
        argumentsNode.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            arguments.put(entry.getKey(), value.isTextual() ? value.asText() : value.toString());
        });
        return arguments;
    }
}
