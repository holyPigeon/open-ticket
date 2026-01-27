package com.example.openticket.api.service.auth;

import com.example.openticket.api.service.auth.dto.response.LoginResponse;
import com.example.openticket.domain.user.User;
import com.example.openticket.domain.user.UserRepository;
import com.example.openticket.global.auth.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtProvider.createToken(user.getId());

        return LoginResponse.from(token);
    }
}