package com.module.facerecognition.auth.dtos;


public record RegisterRequest(
        String name,
        String email,
        String password
) {
}