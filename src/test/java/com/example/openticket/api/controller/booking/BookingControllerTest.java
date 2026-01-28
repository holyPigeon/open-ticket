package com.example.openticket.api.controller.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.openticket.api.controller.booking.dto.request.BookingCreateRequest;
import com.example.openticket.api.service.booking.dto.response.BookingResponse;
import com.example.openticket.domain.booking.BookingStatus;
import com.example.openticket.domain.user.User;
import com.example.openticket.support.ControllerTestSupport;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

class BookingControllerTest extends ControllerTestSupport {

    private static final String ACCESS_TOKEN = "Bearer test_access_token";

    private User user;

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
    }

    @DisplayName("신규 예약을 생성한다.")
    @Test
    void createBooking() throws Exception {
        // given
        BookingCreateRequest request = new BookingCreateRequest(List.of(1L, 2L));

        given(bookingService.createBooking(any(User.class), any(), any(LocalDateTime.class)))
                .willReturn(new BookingResponse(
                        1L, null, List.of(), LocalDateTime.now(), 20000, BookingStatus.BOOKED
                ));

        // when & then
        mockMvc.perform(
                        post("/api/v1/bookings")
                                .header("Authorization", ACCESS_TOKEN)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("200 OK"))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @DisplayName("신규 예약 생성 시 좌석 정보는 필수값이다.")
    @Test
    void createBookingWithoutSeats() throws Exception {
        // given
        BookingCreateRequest request = new BookingCreateRequest(Collections.emptyList());

        // when & then
        mockMvc.perform(
                        post("/api/v1/bookings")
                                .header("Authorization", ACCESS_TOKEN)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.status").value("400 BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("좌석 ID는 필수입니다."));
    }

    @DisplayName("토큰 없이 예약을 요청하면 인증 예외가 발생한다.")
    @Test
    void createBookingWithoutToken() throws Exception {
        // given
        BookingCreateRequest request = new BookingCreateRequest(List.of(1L));

        // when & then
        mockMvc.perform(
                        post("/api/v1/bookings")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"))
                .andExpect(jsonPath("$.status").value("401 UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));
    }
}