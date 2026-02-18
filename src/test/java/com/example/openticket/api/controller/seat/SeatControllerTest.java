package com.example.openticket.api.controller.seat;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.openticket.api.service.event.dto.response.EventResponse;
import com.example.openticket.api.service.seat.dto.response.SeatResponse;
import com.example.openticket.domain.seat.SeatStatus;
import com.example.openticket.domain.user.User;
import com.example.openticket.support.ControllerTestSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SeatControllerTest extends ControllerTestSupport {

    private static final String ACCESS_TOKEN = "Bearer test_access_token";

    @BeforeEach
    void setUp() {
        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserId(anyString())).willReturn(1L);

        User mockUser = User.builder()
                .name("user 1")
                .email("email 1")
                .password("password 1")
                .build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(eventQueueManager.validate(anyLong(), anyString())).willReturn(true);
    }

    @DisplayName("이벤트의 좌석 목록을 조회한다.")
    @Test
    void getSeats() throws Exception {
        // given
        EventResponse eventResponse = new EventResponse(
                1L, "테스트 이벤트", null,
                LocalDateTime.of(2026, 3, 1, 10, 0),
                LocalDateTime.of(2026, 3, 1, 12, 0),
                "테스트 장소"
        );

        List<SeatResponse> seatResponses = List.of(
                new SeatResponse(1L, eventResponse, "A1", 10000, SeatStatus.AVAILABLE),
                new SeatResponse(2L, eventResponse, "A2", 10000, SeatStatus.AVAILABLE)
        );

        given(seatService.getSeats(1L)).willReturn(seatResponses);

        // when & then
        mockMvc.perform(
                        get("/api/v1/events/{eventId}/seats", 1L)
                                .header("Authorization", ACCESS_TOKEN)
                                .header("X-Queue-Token", "test-queue-token")
                                .header("X-Event-Id", "1")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("200 OK"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].seatNumber").value("A1"))
                .andExpect(jsonPath("$.data[1].seatNumber").value("A2"));
    }
}
