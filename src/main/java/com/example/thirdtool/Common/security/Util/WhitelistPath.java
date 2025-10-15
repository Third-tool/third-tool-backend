package com.example.thirdtool.Common.security.Util;

import java.util.List;

public final class WhitelistPath {

    private WhitelistPath() {} // 인스턴스화 방지

    public static final List<String> PATHS = List.of(
            "/actuator/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/join",
            "/login",
            "/oauth2",
            "/kakao",
            "/naver"
                                                    );
}
