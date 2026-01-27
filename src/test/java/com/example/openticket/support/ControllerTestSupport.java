package com.example.openticket.support;

import com.example.openticket.api.controller.booking.BookingController;
import com.example.openticket.api.controller.event.EventController;
import com.example.openticket.api.service.auth.AuthService;
import com.example.openticket.api.service.booking.BookingService;
import com.example.openticket.api.service.event.EventService;
import com.example.openticket.global.auth.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = {
        BookingController.class,
        EventController.class
})
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // 비즈니스 로직 Mock
    @MockitoBean
    protected BookingService bookingService;

    @MockitoBean
    protected EventService eventService;

    @MockitoBean
    protected AuthService authService;

    // 인증 관련 Mock (Interceptor가 사용)
    @MockitoBean
    protected JwtProvider jwtProvider;

}