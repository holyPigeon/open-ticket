package com.example.openticket.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.openticket.global.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {

    @InjectMocks
    private AuthenticationInterceptor authenticationInterceptor;

    @Mock
    private JwtProvider jwtProvider;

    @DisplayName("유효한 토큰이 헤더에 있으면 요청이 통과된다.")
    @Test
    void preHandleSuccess() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(request.getHeader("Authorization")).willReturn("Bearer valid_token");
        given(jwtProvider.validateToken("valid_token")).willReturn(true);
        given(jwtProvider.getUserId("valid_token")).willReturn(1L);

        // when
        boolean result = authenticationInterceptor.preHandle(request, response, new Object());

        // then
        assertThat(result).isTrue();
    }

    @DisplayName("Authorization 헤더가 없으면 예외가 발생한다.")
    @Test
    void preHandleFail_NoHeader() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(request.getHeader("Authorization")).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authenticationInterceptor.preHandle(request, response, new Object()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("유효하지 않은 토큰입니다.");
    }

    @DisplayName("토큰 형식이 Bearer로 시작하지 않으면 예외가 발생한다.")
    @Test
    void preHandleFail_InvalidScheme() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(request.getHeader("Authorization")).willReturn("Basic invalid_scheme_token");

        // when & then
        assertThatThrownBy(() -> authenticationInterceptor.preHandle(request, response, new Object()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("유효하지 않은 토큰입니다.");
    }

    @DisplayName("유효하지 않은 토큰(검증 실패)이면 예외가 발생한다.")
    @Test
    void preHandleFail_InvalidToken() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(request.getHeader("Authorization")).willReturn("Bearer invalid_token");
        given(jwtProvider.validateToken("invalid_token")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authenticationInterceptor.preHandle(request, response, new Object()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("유효하지 않은 토큰입니다.");
    }
}