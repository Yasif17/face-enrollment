package com.module.facerecognition.auth.dtos;

public record AuthResponse(
        String token,
        Long userId,
        String name,
        String email
) {
}