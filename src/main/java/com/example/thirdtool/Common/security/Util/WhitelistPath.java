package com.example.thirdtool.Common.security.Util;

import java.util.List;

public final class WhitelistPath {

    private WhitelistPath() {}

    public static final List<String> PATHS = List.of(
            "/actuator/health",
            "/health",
            "/swagger-ui",         // Swagger 정적 리소스
            "/swagger-ui.html",    // ✅ Swagger HTML 진입점 추가
            "/custom-api-docs",    // ✅ API 문서 경로 추가
            "/v3/api-docs",
            "/join",
            "/login",
            "/oauth2",
            "/kakao",
            "/naver"
                                                    );
}