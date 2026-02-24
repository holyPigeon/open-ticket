package com.example.openticket.api.controller.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.openticket.api.service.event.dto.response.EventResponse;
import com.example.openticket.domain.event.Category;
import com.example.openticket.support.ControllerTestSupport;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class EventControllerTest extends ControllerTestSupport {

    @DisplayName("이벤트 목록을 검색 조건으로 조회한다.")
    @Test
    void searchEvents() throws Exception {
        // given
        List<EventResponse> events = List.of(
                new EventResponse(1L, "콘서트 A", Category.CONCERT,
                        LocalDateTime.of(2026, 3, 1, 10, 0),
                        LocalDateTime.of(2026, 3, 1, 12, 0),
                        "올림픽홀"),
                new EventResponse(2L, "콘서트 B", Category.CONCERT,
                        LocalDateTime.of(2026, 4, 1, 14, 0),
                        LocalDateTime.of(2026, 4, 1, 16, 0),
                        "잠실경기장")
        );
        Page<EventResponse> page = new PageImpl<>(events);

        given(eventService.searchEvents(any(), any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(
                        get("/api/v1/events")
                                .param("category", "CONCERT")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("200 OK"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].title").value("콘서트 A"))
                .andExpect(jsonPath("$.data.content[1].title").value("콘서트 B"));
    }

    @DisplayName("이벤트 상세 정보를 조회한다.")
    @Test
    void getEventDetails() throws Exception {
        // given
        EventResponse response = new EventResponse(
                1L, "콘서트 A", Category.CONCERT,
                LocalDateTime.of(2026, 3, 1, 10, 0),
                LocalDateTime.of(2026, 3, 1, 12, 0),
                "올림픽홀"
        );

        given(eventService.getEventDetails(1L)).willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/events/{eventId}", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("200 OK"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("콘서트 A"))
                .andExpect(jsonPath("$.data.category").value("CONCERT"))
                .andExpect(jsonPath("$.data.venue").value("올림픽홀"));
    }

}
