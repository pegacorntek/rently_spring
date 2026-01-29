package com.pegacorn.rently.controller;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.auth.*;
import com.pegacorn.rently.dto.common.ApiResponse;

import com.pegacorn.rently.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestOtp(@Valid @RequestBody OtpRequest request) {
        authService.requestOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.OTP_SENT_SUCCESS));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.OTP_VERIFIED_SUCCESS));
    }

    @PostMapping("/register/complete")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response, MessageConstant.REGISTER_SUCCESS));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, MessageConstant.LOGIN_SUCCESS));
    }

    @PostMapping("/forgot/request-otp")
    public ResponseEntity<ApiResponse<Void>> forgotPasswordOtp(@RequestBody Map<String, String> body) {
        authService.forgotPasswordOtp(body.get("phone"));
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.OTP_SENT_SUCCESS));
    }

    @PostMapping("/forgot/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.PASSWORD_RESET_SUCCESS));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, MessageConstant.TOKEN_REFRESH_SUCCESS));
    }
}
