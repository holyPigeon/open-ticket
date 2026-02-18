package com.example.openticket.api.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.openticket.api.service.auth.dto.response.LoginResponse;
import com.example.openticket.domain.user.User;
import com.example.openticket.domain.user.UserRepository;
import com.example.openticket.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthServiceTest extends IntegrationTestSupport {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @DisplayName("올바른 이메일과 비밀번호로 로그인하면 JWT 토큰을 반환한다.")
    @Test
    void login() {
        // given
        User user = User.builder()
                .name("홍길동")
                .email("hong@example.com")
                .password("password123")
                .build();
        userRepository.save(user);

        // when
        LoginResponse response = authService.login("hong@example.com", "password123");

        // then
        assertThat(response.token()).isNotBlank();
    }

    @DisplayName("존재하지 않는 이메일로 로그인하면 예외가 발생한다.")
    @Test
    void loginWithUnknownEmail() {
        // when & then
        assertThatThrownBy(() -> authService.login("unknown@example.com", "password123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다.")
    @Test
    void loginWithWrongPassword() {
        // given
        User user = User.builder()
                .name("홍길동")
                .email("hong@example.com")
                .password("password123")
                .build();
        userRepository.save(user);

        // when & then
        assertThatThrownBy(() -> authService.login("hong@example.com", "wrongPassword"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }
}
