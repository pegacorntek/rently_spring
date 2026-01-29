package com.pegacorn.rently.dto.auth;

public record LoginResponse(
        UserDto user,
        String token,
        String refreshToken
) {}
