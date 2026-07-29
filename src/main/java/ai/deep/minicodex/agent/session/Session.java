package ai.deep.minicodex.agent.session;

import java.time.Instant;
import java.util.UUID;

/**
 * 一次 Agent 运行的会话标识。
 *
 * @param id 会话唯一标识
 * @param startedAt 会话创建时间
 */
public record Session(String id, Instant startedAt) {
    /**
     * 创建新的会话。
     *
     * @return 带随机 UUID 的会话
     */
    public static Session create() {
        return new Session(UUID.randomUUID().toString(), Instant.now());
    }
}
