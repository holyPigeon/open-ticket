package com.example.openticket.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test-redis")
@SpringBootTest
@Testcontainers
@EnabledIfDockerAvailable
public abstract class RedisTestSupport {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
                    .withExposedPorts(6379);

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @BeforeEach
    void flushRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }
}
