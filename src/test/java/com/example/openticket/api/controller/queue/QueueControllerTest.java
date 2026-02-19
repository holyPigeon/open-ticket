package com.example.openticket.api.controller.queue;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.openticket.api.service.queue.dto.response.QueueStatusResponse;
import com.example.openticket.domain.user.User;
import com.example.openticket.global.queue.QueuePhase;
import com.example.openticket.support.ControllerTestSupport;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QueueControllerTest extends ControllerTestSupport {

    @DisplayName("대기열에 진입하면 토큰과 상태를 반환한다.")
    @Test
    void enterQueue() throws Exception {
        // given
        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserId(anyString())).willReturn(1L);

        User mockUser = User.builder().name("user").email("email").password("pw").build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        given(userRepository.findById(anyLong())).willReturn(Optional.of(mockUser));

        QueueStatusResponse response = new QueueStatusResponse("test-token", QueuePhase.ALLOWED, 0, 300);
        given(queueService.enterQueue(anyLong(), anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/queue/events/1")
                        .header("Authorization", "Bearer test-jwt"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("test-token"))
                .andExpect(jsonPath("$.data.phase").value("ALLOWED"))
                .andExpect(jsonPath("$.data.remainingSeconds").value(300));
    }

    @DisplayName("대기열 상태를 조회하면 현재 위치와 상태를 반환한다.")
    @Test
    void checkStatus_waiting() throws Exception {
        // given
        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserId(anyString())).willReturn(1L);

        User mockUser = User.builder().name("user").email("email").password("pw").build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        given(userRepository.findById(anyLong())).willReturn(Optional.of(mockUser));

        QueueStatusResponse response = new QueueStatusResponse("test-token", QueuePhase.WAITING, 5, 0);
        given(queueService.checkStatus(anyLong(), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/queue/events/1")
                        .param("token", "test-token")
                        .header("Authorization", "Bearer test-jwt"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("test-token"))
                .andExpect(jsonPath("$.data.phase").value("WAITING"))
                .andExpect(jsonPath("$.data.position").value(5));
    }
}
