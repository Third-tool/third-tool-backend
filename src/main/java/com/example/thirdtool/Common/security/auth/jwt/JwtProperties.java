package com.example.thirdtool.Common.security.auth.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank
        @Size(min = 32, message = "JWT secret-key는 HS256 알고리즘 기준 최소 32바이트(256bit) 이상이어야 합니다")
        String secretKey,

        @NotNull Duration accessTokenTtl,

        @NotNull Duration refreshTokenTtl
) {
}

