package com.example.openticket.infrastructure.queue.redis;

import com.example.openticket.domain.queue.EventQueueManager;
import com.example.openticket.domain.queue.QueueProperties;
import com.example.openticket.domain.queue.QueueStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "queue.type", havingValue = "redis")
public class RedisEventQueueManager implements EventQueueManager {

    private static final String REGISTRY_KEY = "queue:events:registry";

    private static final String PHASE_ALLOWED = "ALLOWED";
    private static final String PHASE_INVALID = "INVALID";

    private final StringRedisTemplate redisTemplate;
    private final QueueProperties properties;

    private final DefaultRedisScript<List> enterScript;
    private final DefaultRedisScript<List> checkScript;
    private final DefaultRedisScript<Long> consumeScript;
    private final DefaultRedisScript<Long> leaveScript;
    private final DefaultRedisScript<Long> promoteScript;

    public RedisEventQueueManager(StringRedisTemplate redisTemplate, QueueProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;

        this.enterScript = loadScript("lua/queue/queue_enter.lua", List.class);
        this.checkScript = loadScript("lua/queue/queue_check.lua", List.class);
        this.consumeScript = loadScript("lua/queue/queue_consume.lua", Long.class);
        this.leaveScript = loadScript("lua/queue/queue_leaveㅇㅇa", Long.class);
        this.promoteScript = loadScript("lua/queue/queue_promote.lua", Long.class);
    }

    @Override
    public QueueStatus enter(Long eventId, Long userId) {
        String token = UUID.randomUUID().toString();
        long nowMs = System.currentTimeMillis();

        List<String> keys = List.of(
                activeKey(eventId), waitingKey(eventId), tokensKey(eventId),
                usersKey(eventId), sequenceKey(eventId), REGISTRY_KEY
        );

        @SuppressWarnings("unchecked")
        List<String> result = redisTemplate.execute(enterScript, keys,
                userId.toString(),
                String.valueOf(properties.maxActivePerEvent()),
                String.valueOf(properties.activeWindowSeconds()),
                String.valueOf(nowMs),
                token,
                eventId.toString()
        );

        validateLuaResult(result, eventId, "진입");

        return parseQueueStatus(result);
    }

    @Override
    public QueueStatus check(Long eventId, String token) {
        int metadataTtl = properties.activeWindowSeconds() * 2;

        List<String> keys = List.of(
                activeKey(eventId), waitingKey(eventId),
                tokensKey(eventId), usersKey(eventId)
        );

        @SuppressWarnings("unchecked")
        List<String> result = redisTemplate.execute(checkScript, keys,
                token,
                String.valueOf(metadataTtl)
        );

        validateLuaResult(result, eventId, "상태 조회");

        if (PHASE_INVALID.equals(result.get(0))) {
            throw new IllegalArgumentException("유효하지 않은 대기열 토큰입니다.");
        }

        return parseQueueStatus(result);
    }

    @Override
    public boolean validate(Long eventId, String token) {
        return redisTemplate.opsForHash().hasKey(activeKey(eventId), token);
    }

    @Override
    public boolean consumeActiveToken(Long eventId, String token) {
        List<String> keys = List.of(
                activeKey(eventId), waitingKey(eventId),
                tokensKey(eventId), usersKey(eventId)
        );

        Long result = redisTemplate.execute(consumeScript, keys,
                token,
                String.valueOf(properties.activeWindowSeconds())
        );

        return result != null && result == 1L;
    }

    @Override
    public boolean leave(Long eventId, String token) {
        List<String> keys = List.of(
                activeKey(eventId), waitingKey(eventId),
                tokensKey(eventId), usersKey(eventId)
        );

        Long result = redisTemplate.execute(leaveScript, keys,
                token,
                String.valueOf(properties.activeWindowSeconds())
        );

        return result != null && result == 1L;
    }

    @Override
    public void promoteForEvent(Long eventId) {
        List<String> keys = List.of(
                activeKey(eventId), waitingKey(eventId),
                tokensKey(eventId), usersKey(eventId), REGISTRY_KEY
        );

        redisTemplate.execute(promoteScript, keys,
                String.valueOf(properties.maxActivePerEvent()),
                String.valueOf(properties.activeWindowSeconds()),
                eventId.toString()
        );
    }

    private String activeKey(Long eventId) {
        return "queue:events:" + eventId + ":active";
    }

    private String waitingKey(Long eventId) {
        return "queue:events:" + eventId + ":waiting";
    }

    private String tokensKey(Long eventId) {
        return "queue:events:" + eventId + ":tokens";
    }

    private String usersKey(Long eventId) {
        return "queue:events:" + eventId + ":users";
    }

    private String sequenceKey(Long eventId) {
        return "queue:events:" + eventId + ":sequence";
    }

    private QueueStatus parseQueueStatus(List<String> result) {
        String returnedToken = result.get(1);
        int position = Integer.parseInt(result.get(2));
        long remainingSeconds = Long.parseLong(result.get(3));
        if (PHASE_ALLOWED.equals(result.get(0))) {
            return QueueStatus.allowed(returnedToken, remainingSeconds);
        }
        return QueueStatus.waiting(returnedToken, position);
    }

    private void validateLuaResult(List<String> result, Long eventId, String operation) {
        if (result == null || result.size() < 4) {
            throw new IllegalStateException(
                    String.format("대기열 %s 중 오류가 발생했습니다. eventId=%d", operation, eventId));
        }
    }

    private static <T> DefaultRedisScript<T> loadScript(String path, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(resultType);
        return script;
    }
}
