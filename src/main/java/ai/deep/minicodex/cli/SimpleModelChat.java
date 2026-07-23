package ai.deep.minicodex.cli;

import ai.deep.minicodex.model.ModelConfig;
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

        System.out.println("用户: " + userMessage);
        System.out.println("HTTP 状态码: " + response.statusCode());
        System.out.println("模型: " + extractContent(response.body()));
    }

    private static HttpResponse<String> sendChatRequest(ModelConfig config, String userMessage)
            throws IOException, InterruptedException {
        String requestBody = buildRequestBody(config, userMessage);
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.url()))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
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

    private static String extractContent(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode content = root.at("/choices/0/message/content");
        if (!content.isMissingNode()) {
            return content.asText();
        }
        return responseBody;
    }
}
