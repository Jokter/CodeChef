package ai.deep.minicodex.model.client;

import ai.deep.minicodex.model.api.ModelClient;
import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.api.ToolCall;
import ai.deep.minicodex.model.config.ModelConfig;
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
 * 调用真实模型接口的模型客户端。
 *
 * <p>该实现使用 {@link ModelConfig} 中的 OpenAI Chat Completions 兼容配置发起请求。
 * 为了适配当前最小 Agent 结构，它要求模型返回一个简单 JSON 对象，用来表示最终回答
 * 或工具调用。</p>
 */
public class RealModelClient implements ModelClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final String SYSTEM_PROMPT = """
            你是一个最小 Java Agent 的真实模型客户端。
            你必须只返回一个 JSON 对象，不要使用 Markdown 代码块。

            可用工具：
            - list_files: 列出目录的直接子项，参数 {"path":"."}
            - read_file: 读取工作区内的普通文件，参数 {"path":"README.md"}

            返回格式只能是以下两种之一：
            {"type":"final","answer":"最终回答"}
            {"type":"tool_call","tool":"工具名","arguments":{"参数名":"参数值"}}

            当需要查看文件或目录后才能回答时，先返回 tool_call。
            当已经可以回答用户任务时，返回 final。
            """;

    private final ModelConfig config;
    private final HttpClient httpClient;

    /**
     * 创建真实模型客户端。
     *
     * @param config 真实模型配置
     */
    public RealModelClient(ModelConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * 调用真实模型生成下一步响应。
     *
     * @param userTask 用户原始任务
     * @param observations 历史工具观察结果
     * @return 最终回答或工具调用
     */
    @Override
    public ModelResponse next(String userTask, List<String> observations) {
        try {
            HttpResponse<String> response = sendChatRequest(userTask, observations);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("真实模型请求失败，HTTP 状态码: "
                        + response.statusCode() + "\n" + response.body());
            }

            String content = extractContent(OBJECT_MAPPER.readTree(response.body()));
            return parseModelResponse(content);
        } catch (IOException e) {
            throw new IllegalStateException("真实模型请求或响应解析失败。", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("真实模型请求被中断。", e);
        }
    }

    private HttpResponse<String> sendChatRequest(String userTask, List<String> observations)
            throws IOException, InterruptedException {
        String requestBody = buildRequestBody(userTask, observations);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(config.url()))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (config.hasApiKey()) {
            requestBuilder.header("Authorization", "Bearer " + config.apiKey());
        }

        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String buildRequestBody(String userTask, List<String> observations) throws JsonProcessingException {
        Map<String, Object> payload = Map.of(
                "model", config.name(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", buildUserContent(userTask, observations))
                ),
                "temperature", 0.2,
                "max_tokens", 600
        );
        return OBJECT_MAPPER.writeValueAsString(payload);
    }

    private String buildUserContent(String userTask, List<String> observations) {
        if (observations.isEmpty()) {
            return "用户任务:\n" + userTask + "\n\n当前还没有工具观察结果。";
        }

        return """
                用户任务:
                %s

                历史工具观察结果:
                %s
                """.formatted(userTask, String.join("\n---\n", observations));
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
