package com.example.thirdtool.Common.security.filter;


import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.Util.JWTUtil;
import com.example.thirdtool.Common.Util.WhitelistPath;
import com.example.thirdtool.Common.security.auth.token.TokenType;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private static final String ACCESS_COOKIE_NAME = "access_token";

    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // ✅ Health Check 요청이면 로그 남기지 않고 통과
        if (requestUri.equals("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("[JWTFilter] 요청 URI: {}", requestUri);

        // 🚨 PHP / ASPX / 기타 악성 패턴 빠른 차단 (Story 3-3에서 BlockListFilter로 분리 예정)
        if (requestUri.endsWith(".php") || requestUri.endsWith(".aspx") ||
                requestUri.contains("/wp-") || requestUri.contains("/cgi-bin/")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // ✅ JWT 검증을 건너뛸 경로 (화이트리스트)
        if (isExcludedPath(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1️⃣ Cookie 배열에서 access_token 추출 (Story 1-4 — Authorization 헤더 폐기)
        String accessToken = extractAccessTokenFromCookie(request);
        if (accessToken == null) {
            log.debug("[JWTFilter] access_token 쿠키 없음 → 다음 필터로 진행 (익명 처리)");
            filterChain.doFilter(request, response);
            return;
        }

        // 2️⃣ JWT 유효성 검증
        try {
            if (jwtUtil.isValid(accessToken, TokenType.ACCESS)) {
                String username = jwtUtil.getUsername(accessToken);
                String role = jwtUtil.getRole(accessToken);

                // 3️⃣ DB에서 사용자 확인
                UserEntity user = userRepository.findByUsername(username)
                                                .orElseThrow(() -> {
                                                    log.warn("[JWTFilter] 토큰의 user를 DB에서 찾을 수 없음: {}", username);
                                                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                                                });

                // 4️⃣ SecurityContext에 인증정보 저장
                List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
                Authentication auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("[JWTFilter] SecurityContextHolder 인증 성공 - {}", username);
                filterChain.doFilter(request, response);
                return;
            } else {
                log.warn("[JWTFilter] access_token 유효성 검증 실패 (만료 또는 위조)");
                writeUnauthorizedResponse(response, "토큰 만료 또는 유효하지 않은 토큰");
                return;
            }

        } catch (BusinessException be) {
            // BusinessException은 GlobalExceptionHandler가 처리. Story 3-1에서 EntryPoint 정리 후 throw로 전환 예정
            log.warn("[JWTFilter] 인증 처리 중 비즈니스 예외: {}", be.getErrorCode());
            writeUnauthorizedResponse(response, be.getMessage());
        } catch (Exception e) {
            log.error("[JWTFilter] JWT 검증 중 예외: {}", e.getMessage(), e);
            writeUnauthorizedResponse(response, "JWT 검증 중 오류 발생");
        }
    }

    /**
     * Cookie 배열에서 access_token 쿠키 값을 추출한다.
     * 쿠키 없거나 access_token 키가 없으면 null 반환 (익명 요청 통과 경로).
     */
    private String extractAccessTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (ACCESS_COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                return (value == null || value.isBlank()) ? null : value;
            }
        }
        return null;
    }

    /**
     * actuator / swagger / public API 등 JWT 검증 제외 경로
     */
    private boolean isExcludedPath(String uri) {
        return WhitelistPath.PATHS.stream().anyMatch(uri::contains);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        // Story 3-1에서 AuthenticationEntryPoint + GlobalExceptionHandler로 정리 예정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

}
