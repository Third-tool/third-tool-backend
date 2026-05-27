package com.example.thirdtool.Common.security.auth.token;

import com.example.thirdtool.Common.Util.JWTUtil;
import com.example.thirdtool.Common.security.auth.RefreshEntity;
import com.example.thirdtool.Common.security.auth.RefreshRepository;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JwtTokenIssuer implements TokenIssuer {

    private static final String ACCESS_COOKIE_NAME = "access_token";

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    public JwtTokenIssuer(JWTUtil jwtUtil, RefreshRepository refreshRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
    }

    @Override
    @Transactional
    public String issue(UserEntity user, HttpServletResponse response) {
        String username = user.getUsername();
        String role = "ROLE_" + user.getRoleType().name();
        return doIssue(username, role, response);
    }

    @Override
    @Transactional
    public String reissue(String username, String role, HttpServletResponse response) {
        return doIssue(username, role, response);
    }

    private String doIssue(String username, String role, HttpServletResponse response) {
        String accessToken = jwtUtil.createJWT(username, role, jwtUtil.accessTokenTtl(), TokenType.ACCESS);
        String refreshToken = jwtUtil.createJWT(username, role, jwtUtil.refreshTokenTtl(), TokenType.REFRESH);

        writeAccessTokenCookie(response, accessToken);
        upsertRefreshWhitelist(username, refreshToken);

        return refreshToken;
    }

    private void writeAccessTokenCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_COOKIE_NAME, accessToken)
                                              .httpOnly(true)
                                              // Story 1-3에서 프로파일별 외부화 예정. 1-2에서는 안전한 기본값.
                                              .secure(false)
                                              .sameSite("Strict")
                                              .path("/")
                                              .maxAge(jwtUtil.accessTokenTtl())
                                              .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void upsertRefreshWhitelist(String username, String refreshToken) {
        // Story 2-3에서 RefreshEntity.updateRefresh() 도메인 메서드로 정리 예정.
        // 본 Story는 기존 JwtService.addRefresh 로직을 그대로 흡수.
        RefreshEntity existing = refreshRepository.findEntityByUsername(username)
                                                  .orElse(null);
        RefreshEntity entity = RefreshEntity.builder()
                                            .id(existing != null ? existing.getId() : null)
                                            .username(username)
                                            .refresh(refreshToken)
                                            .build();
        refreshRepository.save(entity);
    }
}
