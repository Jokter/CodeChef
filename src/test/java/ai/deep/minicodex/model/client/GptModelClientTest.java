package ai.deep.minicodex.model.client;

import ai.deep.minicodex.model.api.ModelContext;
import ai.deep.minicodex.model.api.ModelResponse;
import ai.deep.minicodex.model.config.GptModelConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GptModelClientTest {
    @Test
    void retriesTemporaryModelFailuresUntilSuccess() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (TestModelServer server = TestModelServer.start(exchange -> {
            int attempt = calls.incrementAndGet();
            if (attempt < 3) {
                respond(exchange, 500, "{\"error\":\"temporary\"}");
                return;
            }

            respond(exchange, 200, openAiResponse("{\"type\":\"final\",\"answer\":\"完成\"}"));
        })) {
            GptModelClient client = new GptModelClient(config(server.url()));

            ModelResponse response = client.next(context());

            assertTrue(response.isFinalAnswer());
            assertEquals("完成", response.finalAnswer());
            assertEquals(3, calls.get());
        }
    }

    @Test
    void stopsAfterThreeTemporaryModelFailures() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (TestModelServer server = TestModelServer.start(exchange -> {
            calls.incrementAndGet();
            respond(exchange, 500, "{\"error\":\"temporary\"}");
        })) {
            GptModelClient client = new GptModelClient(config(server.url()));

            IllegalStateException error = assertThrows(IllegalStateException.class, () -> client.next(context()));

            assertTrue(error.getMessage().contains("已尝试 3 次"));
            assertEquals(3, calls.get());
        }
    }

    @Test
    void doesNotRetryClientErrors() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (TestModelServer server = TestModelServer.start(exchange -> {
            calls.incrementAndGet();
            respond(exchange, 400, "{\"error\":\"bad request\"}");
        })) {
            GptModelClient client = new GptModelClient(config(server.url()));

            IllegalStateException error = assertThrows(IllegalStateException.class, () -> client.next(context()));

            assertTrue(error.getMessage().contains("HTTP 状态码: 400"));
            assertEquals(1, calls.get());
        }
    }

    private static GptModelConfig config(String url) {
        return new GptModelConfig("test-model", url, "openai");
    }

    private static ModelContext context() {
        return new ModelContext("用户任务", List.of(), "系统提示", "用户内容");
    }

    private static String openAiResponse(String content) {
        String escapedContent = content.replace("\\", "\\\\").replace("\"", "\\\"");
        return """
                {"choices":[{"message":{"content":"%s"}}]}
                """.formatted(escapedContent);
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static class TestModelServer implements AutoCloseable {
        private final HttpServer server;

        private TestModelServer(HttpServer server) {
            this.server = server;
        }

        static TestModelServer start(HttpHandler handler) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/", handler);
            server.start();
            return new TestModelServer(server);
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
