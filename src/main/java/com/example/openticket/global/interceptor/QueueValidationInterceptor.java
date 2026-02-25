package com.example.openticket.global.interceptor;

import com.example.openticket.domain.queue.EventQueueManager;
import com.example.openticket.global.annotation.CheckQueueToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

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
        validateEventIdConsistency(request, eventId);
        boolean valid = annotation.consume()
                ? eventQueueManager.consumeActiveToken(eventId, token)
                : eventQueueManager.validate(eventId, token);
        if (!valid) {
            throw new IllegalStateException("대기열 토큰이 유효하지 않거나 만료되었습니다.");
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private void validateEventIdConsistency(HttpServletRequest request, Long headerEventId) {
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
        );
        if (pathVariables == null || !pathVariables.containsKey("eventId")) {
            return;
        }
        Long pathEventId = Long.parseLong(pathVariables.get("eventId"));
        if (!Objects.equals(pathEventId, headerEventId)) {
            throw new IllegalStateException("대기열 토큰의 이벤트 ID가 요청 경로의 이벤트 ID와 일치하지 않습니다.");
        }
    }

    private String extractRequiredHeader(HttpServletRequest request, String headerName, String errorMessage) {
        String value = request.getHeader(headerName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(errorMessage);
        }
        return value;
    }
}
