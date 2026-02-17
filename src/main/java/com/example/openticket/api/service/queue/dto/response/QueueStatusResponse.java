package com.example.openticket.api.service.queue.dto.response;

import com.example.openticket.global.queue.EventQueueManager.QueuePhase;
import com.example.openticket.global.queue.EventQueueManager.QueueStatus;

public record QueueStatusResponse(
        String token,
        QueuePhase phase,
        int position,
        long remainingSeconds
) {

    public static QueueStatusResponse from(QueueStatus status) {
        return new QueueStatusResponse(
                status.token(),
                status.phase(),
                status.position(),
                status.remainingSeconds()
        );
    }
}
