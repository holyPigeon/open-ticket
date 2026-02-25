package com.example.openticket.infrastructure.queue.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "queue.type", havingValue = "redis")
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(DataRedisConnectionDetails connectionDetails) {
        DataRedisConnectionDetails.Standalone standalone = connectionDetails.getStandalone();
        String host = standalone.getHost();
        int port = standalone.getPort();

        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(16);
        return Redisson.create(config);
    }
}
