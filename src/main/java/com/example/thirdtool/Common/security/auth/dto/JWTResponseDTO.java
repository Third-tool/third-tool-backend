package com.example.thirdtool.Common.security.auth.dto;

import lombok.Builder;

@Builder
public record JWTResponseDTO(String accessToken, String refreshToken) {
}
