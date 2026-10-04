package com.module.facerecognition.auth.dtos;

public record LoginRequest(
        String email,
        String password
) {
}