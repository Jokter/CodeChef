package ai.deep.minicodex.model.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * GPT 模型配置。
 *
 * <p>该类从 {@code config/model.properties} 读取 GPT 模型名称、接口地址和请求格式。
 * 后续接入其他模型时，应新增对应配置类与 ModelClient 实现。</p>
 *
 * @param name GPT 模型名称
 * @param url GPT 模型接口地址
 * @param apiFormat GPT 接口请求格式，例如 {@code anthropic} 或 {@code openai}
 */
public record GptModelConfig(String name, String url, String apiFormat) {
    // GPT 模型名称配置项，例如 gpt-5。
    private static final String NAME_KEY = "gpt.model.name";
    // GPT 模型接口地址配置项，例如本地代理的 /v1/messages 地址。
    private static final String URL_KEY = "gpt.model.url";
    // GPT 接口请求格式配置项，目前支持 anthropic 和 openai。
    private static final String API_FORMAT_KEY = "gpt.model.apiFormat";

    /**
     * 从默认路径 {@code config/model.properties} 加载 GPT 模型配置。
     *
     * @return GPT 模型配置
     * @throws IOException 配置文件读取失败时抛出
     * @throws IllegalArgumentException 必填配置项缺失或为空时抛出
     */
    public static GptModelConfig loadDefault() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(Path.of("config", "model.properties"))) {
            properties.load(inputStream);
        }

        return new GptModelConfig(
                required(properties, NAME_KEY),
                required(properties, URL_KEY),
                required(properties, API_FORMAT_KEY)
        );
    }

    /**
     * 判断当前配置是否使用 Anthropic Messages 请求格式。
     *
     * @return 使用 Anthropic 格式时返回 {@code true}
     */
    public boolean isAnthropicApiFormat() {
        return "anthropic".equalsIgnoreCase(apiFormat);
    }

    /**
     * 判断当前配置是否使用 OpenAI Chat Completions 请求格式。
     *
     * @return 使用 OpenAI 格式时返回 {@code true}
     */
    public boolean isOpenAiApiFormat() {
        return "openai".equalsIgnoreCase(apiFormat);
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少 GPT 模型配置项: " + key);
        }
        return value.trim();
    }
}
