package tn.esprit.spring.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.userservice.dto.request.LoginRequest;
import tn.esprit.spring.userservice.dto.request.RefreshTokenRequest;
import tn.esprit.spring.userservice.dto.request.ResendVerificationEmailRequest;
import tn.esprit.spring.userservice.dto.request.ForgotPasswordRequest;
import tn.esprit.spring.userservice.dto.response.LoginResponse;
import tn.esprit.spring.userservice.dto.response.TokenRefreshResponse;
import tn.esprit.spring.userservice.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenRefreshResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.getRefreshToken());
    }

    @PostMapping("/resend-verification")
    public Map<String, String> resendVerification(@Valid @RequestBody ResendVerificationEmailRequest request) {
        authService.resendVerificationEmail(request);
        return Map.of("message", "If an unverified account with an email exists, a verification email has been sent.");
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return Map.of("message", "If the account exists, a recovery email has been sent.");
    }
}
