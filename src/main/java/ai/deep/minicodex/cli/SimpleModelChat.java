package ai.deep.minicodex.cli;

import ai.deep.minicodex.model.config.ModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 简单真实模型对话入口。
 *
 * <p>该类从 {@code config/model.properties} 读取真实模型配置，向配置的
 * {@code model.url} 发起一次普通聊天请求，并打印用户消息和模型回复。
 * 请求和响应均按 OpenAI Chat Completions 兼容格式处理。
 * 它用于比连接探测更直观地验证模型是否能完成正常对话。</p>
 */
public class SimpleModelChat {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_MESSAGE = "你好，请用两句话介绍一下你自己。";

    /**
     * 发起一次简单对话。
     *
     * @param args 可选用户消息；为空时使用默认消息
     * @throws Exception 配置读取、网络请求或响应解析失败时抛出
     */
    public static void main(String[] args) throws Exception {
        ModelConfig config = ModelConfig.loadDefault();
        String userMessage = args.length > 0 ? String.join(" ", args) : DEFAULT_MESSAGE;
        HttpResponse<String> response = sendChatRequest(config, userMessage);

        printResponse(userMessage, response);
    }

    private static HttpResponse<String> sendChatRequest(ModelConfig config, String userMessage)
            throws IOException, InterruptedException {
        String requestBody = buildRequestBody(config, userMessage);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(config.url()))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (config.hasApiKey()) {
            requestBuilder.header("Authorization", "Bearer " + config.apiKey());
        }

        return HttpClient.newHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String buildRequestBody(ModelConfig config, String userMessage) throws IOException {
        Map<String, Object> payload = Map.of(
                "model", config.name(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是一个简洁、友好的中文助手。"),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.7,
                "max_tokens", 200
        );
        return OBJECT_MAPPER.writeValueAsString(payload);
    }

    private static void printResponse(String userMessage, HttpResponse<String> response) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(response.body());

        System.out.println("========== 模型对话 ==========");
        System.out.println("[请求]");
        System.out.println("用户输入: " + userMessage);
        System.out.println();

        System.out.println("[状态]");
        System.out.println("HTTP 状态码: " + response.statusCode());
        printIfPresent("响应 ID", root.path("id"));
        printIfPresent("对象类型", root.path("type"));
        printIfPresent("消息角色", root.path("role"));
        printIfPresent("模型名称", root.path("model"));
        printIfPresent("停止原因", root.path("stop_reason"));
        System.out.println();

        System.out.println("[回复]");
        System.out.println(extractContent(root));
        printUsage(root.path("usage"));
        System.out.println("================================");
    }

    private static String extractContent(JsonNode root) {
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

    private static void printUsage(JsonNode usage) {
        if (usage.isMissingNode() || usage.isNull() || usage.isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println("[用量]");
        printIfPresent("输入 token", usage.path("input_tokens"));
        printIfPresent("输出 token", usage.path("output_tokens"));
        printIfPresent("缓存读取 token", usage.path("cache_read_input_tokens"));
        printIfPresent("缓存写入 token", usage.path("cache_creation_input_tokens"));
    }

    private static void printIfPresent(String label, JsonNode value) {
        if (!value.isMissingNode() && !value.isNull()) {
            System.out.println(label + ": " + value.asText());
        }
    }
}
