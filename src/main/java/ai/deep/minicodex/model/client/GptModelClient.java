package ai.deep.minicodex.model.client;

import ai.deep.minicodex.model.api.ModelClient;
import ai.deep.minicodex.model.api.ModelContext;
import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.model.config.GptModelConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用 GPT 真实接口的模型客户端。
 *
 * <p>该实现使用 {@link GptModelConfig} 中的 GPT 配置发起请求。为了适配当前最小 Agent
 * 结构，它要求模型返回一个简单 JSON 对象，用来表示最终回答或工具调用。</p>
 */
public class GptModelClient implements ModelClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final GptModelConfig config;
    private final HttpClient httpClient;

    /**
     * 创建 GPT 模型客户端。
     *
     * @param config GPT 模型配置
     */
    public GptModelClient(GptModelConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * 调用 GPT 模型生成下一步响应。
     *
     * @param context 模型请求上下文
     * @return 最终回答或工具调用
     */
    @Override
    public ModelResponse next(ModelContext context) {
        try {
            HttpResponse<String> response = sendChatRequest(context);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("GPT 模型请求失败，HTTP 状态码: "
                        + response.statusCode() + "\n" + response.body());
            }

            String content = extractContent(OBJECT_MAPPER.readTree(response.body()));
            return parseModelResponse(content);
        } catch (IOException e) {
            throw new IllegalStateException("GPT 模型请求或响应解析失败。", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GPT 模型请求被中断。", e);
        }
    }

    private HttpResponse<String> sendChatRequest(ModelContext context)
            throws IOException, InterruptedException {
        String requestBody = buildRequestBody(context);
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.url()))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String buildRequestBody(ModelContext context) throws JsonProcessingException {
        if (config.isAnthropicApiFormat()) {
            return buildAnthropicRequestBody(context);
        }

        if (config.isOpenAiApiFormat()) {
            return buildOpenAiRequestBody(context);
        }

        throw new IllegalArgumentException("不支持的 GPT API 请求格式: " + config.apiFormat());
    }

    private String buildAnthropicRequestBody(ModelContext context) throws JsonProcessingException {
        Map<String, Object> payload = Map.of(
                "model", config.name(),
                "system", context.systemPrompt(),
                "messages", List.of(
                        Map.of("role", "user", "content", context.userContent())
                ),
                "temperature", 0.2,
                "max_tokens", 600
        );
        return OBJECT_MAPPER.writeValueAsString(payload);
    }

    private String buildOpenAiRequestBody(ModelContext context) throws JsonProcessingException {
        Map<String, Object> payload = Map.of(
                "model", config.name(),
                "messages", List.of(
                        Map.of("role", "system", "content", context.systemPrompt()),
                        Map.of("role", "user", "content", context.userContent())
                ),
                "temperature", 0.2,
                "max_tokens", 600
        );
        return OBJECT_MAPPER.writeValueAsString(payload);
    }

    private ModelResponse parseModelResponse(String content) throws JsonProcessingException {
        JsonNode root = OBJECT_MAPPER.readTree(extractJsonObject(content));
        String type = root.path("type").asText();

        if ("tool_call".equals(type)) {
            return ModelResponse.toolCall(new ToolCall(
                    root.path("tool").asText(),
                    readStringArguments(root.path("arguments"))
            ));
        }

        if ("final".equals(type)) {
            return ModelResponse.finalAnswer(root.path("answer").asText());
        }

        return ModelResponse.finalAnswer(content);
    }

    private Map<String, String> readStringArguments(JsonNode argumentsNode) {
        Map<String, String> arguments = new LinkedHashMap<>();
        if (!argumentsNode.isObject()) {
            return arguments;
        }

        argumentsNode.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            arguments.put(entry.getKey(), value.isTextual() ? value.asText() : value.toString());
        });
        return arguments;
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

    private String extractContent(JsonNode root) {
        JsonNode content = root.at("/choices/0/message/content");
        if (!content.isMissingNode()) {
            return content.asText();
        }

        JsonNode messageText = root.at("/content/0/text");
        if (!messageText.isMissingNode()) {
            return messageText.asText();
        }
        return root.toPrettyString();
    }
}
