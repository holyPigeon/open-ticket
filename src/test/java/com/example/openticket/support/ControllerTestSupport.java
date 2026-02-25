package com.example.openticket.support;

import com.example.openticket.api.controller.auth.AuthController;
import com.example.openticket.api.controller.booking.BookingController;
import com.example.openticket.api.controller.event.EventController;
import com.example.openticket.api.controller.queue.QueueController;
import com.example.openticket.api.controller.seat.SeatController;
import com.example.openticket.api.service.auth.AuthService;
import com.example.openticket.api.service.booking.BookingService;
import com.example.openticket.api.service.event.EventService;
import com.example.openticket.api.service.queue.QueueService;
import com.example.openticket.api.service.seat.SeatService;
import com.example.openticket.domain.user.UserRepository;
import com.example.openticket.global.auth.JwtProvider;
import com.example.openticket.domain.queue.EventQueueManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = {
        AuthController.class,
        BookingController.class,
        EventController.class,
        QueueController.class,
        SeatController.class
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

    @MockitoBean
    protected QueueService queueService;

    @MockitoBean
    protected SeatService seatService;

    @MockitoBean
    protected UserRepository userRepository;

    // 인증 관련 Mock (Interceptor가 사용)
    @MockitoBean
    protected JwtProvider jwtProvider;

    @MockitoBean
    protected EventQueueManager eventQueueManager;

}
