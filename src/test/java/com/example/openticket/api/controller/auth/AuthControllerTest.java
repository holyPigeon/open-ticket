package com.example.openticket.api.controller.auth;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.openticket.api.controller.auth.dto.request.LoginRequest;
import com.example.openticket.api.service.auth.dto.response.LoginResponse;
import com.example.openticket.support.ControllerTestSupport;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthControllerTest extends ControllerTestSupport {

    @DisplayName("이메일과 비밀번호로 로그인하면 토큰을 반환한다.")
    @Test
    void login() throws Exception {
        // given
        given(authService.login(anyString(), anyString()))
                .willReturn(LoginResponse.from("jwt-token-value"));

        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new LoginRequest("user@example.com", "password123")
                                ))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("200 OK"))
                .andExpect(jsonPath("$.data.token").value("jwt-token-value"));
    }
}
