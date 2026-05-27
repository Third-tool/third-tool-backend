package com.example.thirdtool.Common.security.auth.jwt;

import com.example.thirdtool.Common.security.auth.dto.RefreshRequestDTO;
import com.example.thirdtool.Common.security.auth.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwtController {

    private final JwtService jwtService;

    public JwtController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // 소셜 로그인 쿠키 방식의 Refresh 토큰 헤더 방식으로 교환 — Story 2-2에서 제거 예정
    @PostMapping(value = "/jwt/exchange", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse jwtExchangeApi(
            HttpServletRequest request,
            HttpServletResponse response
                                        ) {
        return jwtService.cookie2Header(request, response);
    }

    // Refresh 토큰으로 Access 토큰 재발급 (Rotate 포함) — AT는 Set-Cookie, 새 RT는 응답 바디
    @PostMapping(value = "/jwt/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse jwtRefreshApi(
            @Validated @RequestBody RefreshRequestDTO dto,
            HttpServletResponse response
                                       ) {
        return jwtService.refreshRotate(dto, response);
    }

}