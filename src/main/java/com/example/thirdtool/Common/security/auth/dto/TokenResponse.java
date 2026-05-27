package com.example.thirdtool.Common.security.auth.dto;

/**
 * 로그인 / 토큰 재발급 응답 바디.
 *
 * Access Token은 응답 바디에 포함되지 않는다 — HttpOnly Cookie로 발급된다 (ADR009).
 * 본 DTO는 클라이언트가 메모리에서 관리할 Refresh Token만 담는다.
 */
public record TokenResponse(String refreshToken) {
}
