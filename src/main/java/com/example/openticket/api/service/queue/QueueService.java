package com.example.openticket.api.service.queue;

import com.example.openticket.api.service.queue.dto.response.QueueStatusResponse;
import com.example.openticket.api.service.queue.dto.response.QueueLeaveResponse;
import com.example.openticket.global.queue.EventQueueManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final EventQueueManager queueManager;

    public QueueStatusResponse enterQueue(Long eventId, Long userId) {
        String token = queueManager.enter(eventId, userId).token();
        return QueueStatusResponse.from(queueManager.check(eventId, token));
    }

    public QueueStatusResponse checkStatus(Long eventId, String token) {
        return QueueStatusResponse.from(queueManager.check(eventId, token));
    }

    public QueueLeaveResponse leaveQueue(Long eventId, String token) {
        return new QueueLeaveResponse(queueManager.leave(eventId, token));
    }
}
