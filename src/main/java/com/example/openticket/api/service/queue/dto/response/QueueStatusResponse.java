package com.example.openticket.api.service.queue.dto.response;

import com.example.openticket.domain.queue.QueuePhase;
import com.example.openticket.domain.queue.QueueStatus;

public record QueueStatusResponse(
        String token,
        QueuePhase phase,
        int position,
        long remainingSeconds,
        int pollIntervalSeconds
) {

    public static QueueStatusResponse from(QueueStatus status) {
        return new QueueStatusResponse(
                status.token(),
                status.phase(),
                status.position(),
                status.remainingSeconds(),
                computePollInterval(status)
        );
    }

    private static int computePollInterval(QueueStatus status) {
        if (status.phase() == QueuePhase.ALLOWED) return 0;
        if (status.position() < 1000)  return 3;
        if (status.position() < 10000) return 10;
        return 30;
    }
}
