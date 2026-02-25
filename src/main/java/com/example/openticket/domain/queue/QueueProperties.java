package com.example.openticket.domain.queue;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue")
public record QueueProperties(
        int maxActivePerEvent,
        int activeWindowSeconds
) {
    public QueueProperties {
        if (maxActivePerEvent <= 0) maxActivePerEvent = 100;
        if (activeWindowSeconds <= 0) activeWindowSeconds = 600;
    }
}
