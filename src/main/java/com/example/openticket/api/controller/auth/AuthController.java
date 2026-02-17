package com.example.openticket.api.controller.auth;

import com.example.openticket.api.ApiResponse;
import com.example.openticket.api.service.auth.AuthService;
import com.example.openticket.api.service.auth.dto.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/v1/auth/login")
    public ApiResponse<LoginResponse> login(String email, String password) {
        return ApiResponse.ok(authService.login(email, password));
    }
}
