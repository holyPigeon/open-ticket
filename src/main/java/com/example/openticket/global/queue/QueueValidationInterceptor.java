package com.example.openticket.global.queue;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class QueueValidationInterceptor implements HandlerInterceptor {

    private static final String QUEUE_TOKEN_HEADER = "X-Queue-Token";
    private static final String EVENT_ID_HEADER = "X-Event-Id";

    private final EventQueueManager eventQueueManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        CheckQueueToken annotation = handlerMethod.getMethodAnnotation(CheckQueueToken.class);
        if (annotation == null) {
            return true;
        }

        String token = extractRequiredHeader(request, QUEUE_TOKEN_HEADER, "대기열 토큰이 필요합니다.");
        Long eventId = Long.parseLong(extractRequiredHeader(request, EVENT_ID_HEADER, "이벤트 ID가 필요합니다."));
        boolean valid = annotation.consume()
                ? eventQueueManager.consumeActiveToken(eventId, token)
                : eventQueueManager.validate(eventId, token);
        if (!valid) {
            throw new IllegalStateException("대기열 토큰이 유효하지 않거나 만료되었습니다.");
        }

        return true;
    }

    private String extractRequiredHeader(HttpServletRequest request, String headerName, String errorMessage) {
        String value = request.getHeader(headerName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(errorMessage);
        }
        return value;
    }
}
