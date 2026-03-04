package com.example.openticket.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test-redis")
@SpringBootTest
@EnabledIfDockerAvailable
@Execution(ExecutionMode.SAME_THREAD)
public abstract class RedisTestSupport {

    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis;

    static {
        redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                .withExposedPorts(6379);
        redis.start();
    }

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @BeforeEach
    void flushRedis() {
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
    }
}
