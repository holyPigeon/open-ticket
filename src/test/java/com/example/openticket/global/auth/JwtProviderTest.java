package com.example.openticket.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "this-is-test-secret-key-at-least-32-chars-long";
    private static final long EXPIRY = 3600000;

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, EXPIRY);

    @DisplayName("사용자 ID로 토큰을 생성하고, 생성된 토큰에서 ID를 추출할 수 있다.")
    @Test
    void createAndParseToken() {
        // given
        Long userId = 1L;

        // when
        String token = jwtProvider.createToken(userId);
        Long extractedId = jwtProvider.getUserId(token);

        // then
        assertThat(token).isNotNull();
        assertThat(extractedId).isEqualTo(userId);
    }

    @DisplayName("유효한 토큰인지 검증할 수 있다.")
    @Test
    void validateToken() {
        // given
        String token = jwtProvider.createToken(1L);

        // when
        boolean isValid = jwtProvider.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @DisplayName("잘못된 토큰은 검증에 실패한다.")
    @Test
    void validateInvalidToken() {
        // given
        String invalidToken = "invalid.token.string";

        // when
        boolean isValid = jwtProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }
}