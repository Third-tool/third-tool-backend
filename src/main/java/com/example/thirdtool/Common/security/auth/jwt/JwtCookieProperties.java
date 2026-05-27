package com.example.thirdtool.Common.security.auth.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Access Token 쿠키 보안 속성 외부화.
 *
 * - `prod` 프로파일은 반드시 secure=true, sameSite=Strict
 * - `local`/`dev` 프로파일은 HTTPS 없는 환경을 위해 secure=false 허용
 *
 * SameSite는 RFC 6265bis 가능 값 (Strict, Lax, None)만 허용.
 */
@Validated
@ConfigurationProperties(prefix = "jwt.cookie")
public record JwtCookieProperties(
        @NotBlank String name,
        @NotBlank String path,
        @NotNull Boolean httpOnly,
        @NotNull Boolean secure,
        @NotBlank
        @Pattern(regexp = "Strict|Lax|None", message = "SameSite는 Strict/Lax/None 중 하나여야 합니다")
        String sameSite
) {
}
