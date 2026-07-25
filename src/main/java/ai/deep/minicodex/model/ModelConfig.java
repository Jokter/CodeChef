package ai.deep.minicodex.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 真实模型配置。
 *
 * <p>该类从 {@code config/model.properties} 读取模型名称、完整 API 地址和 API Key。
 * 它只负责加载和校验配置，不负责发起网络请求。后续接入真实模型客户端时，
 * 可以把该对象传给新的 {@link ModelClient} 实现。</p>
 *
 * @param name 模型名称
 * @param url 模型完整 API 地址，例如 {@code https://www.dmxapi.cn/v1/chat/completions}
 * @param apiKey 模型 API Key；本地代理托管鉴权时可为空
 */
public record ModelConfig(String name, String url, String apiKey) {
    private static final String NAME_KEY = "model.name";
    private static final String URL_KEY = "model.url";
    private static final String API_KEY_KEY = "model.apiKey";

    /**
     * 从默认路径 {@code config/model.properties} 加载模型配置。
     *
     * @return 模型配置
     * @throws IOException 配置文件读取失败时抛出
     * @throws IllegalArgumentException 必填配置项缺失或为空时抛出
     */
    public static ModelConfig loadDefault() throws IOException {
        return load(Path.of("config", "model.properties"));
    }

    /**
     * 从指定路径加载模型配置。
     *
     * @param configPath 配置文件路径
     * @return 模型配置
     * @throws IOException 配置文件读取失败时抛出
     * @throws IllegalArgumentException 必填配置项缺失或为空时抛出
     */
    public static ModelConfig load(Path configPath) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        }

        return new ModelConfig(
                required(properties, NAME_KEY),
                required(properties, URL_KEY),
                optional(properties, API_KEY_KEY, "")
        );
    }

    /**
     * 判断当前配置是否提供了可发送给上游服务的 API Key。
     *
     * @return API Key 非空时返回 {@code true}
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少模型配置项: " + key);
        }
        return value.trim();
    }

    private static String optional(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
