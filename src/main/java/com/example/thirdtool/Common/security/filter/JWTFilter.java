package com.example.thirdtool.Common.security.filter;


import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.Util.JWTUtil;
import com.example.thirdtool.Common.Util.WhitelistPath;
import com.example.thirdtool.Common.security.auth.JwtAuthenticationEntryPoint;
import com.example.thirdtool.Common.security.auth.token.TokenType;
import com.example.thirdtool.Common.security.auth.token.TokenValidationResult;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        if (requestUri.equals("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("[JWTFilter] 요청 URI: {}", requestUri);

        // 악성 URL 차단 (Story 3-3에서 BlockListFilter로 분리 예정)
        if (requestUri.endsWith(".php") || requestUri.endsWith(".aspx") ||
                requestUri.contains("/wp-") || requestUri.contains("/cgi-bin/")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (isExcludedPath(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = extractAccessTokenFromCookie(request);
        if (accessToken == null) {
            // 쿠키가 없는 익명 요청은 통과시키고, 보호 자원이면 EntryPoint에서 AUTH_TOKEN_MISSING으로 응답
            filterChain.doFilter(request, response);
            return;
        }

        try {
            TokenValidationResult validation = jwtUtil.classify(accessToken, TokenType.ACCESS);
            switch (validation) {
                case EXPIRED -> {
                    rejectWithErrorCode(request, response, ErrorCode.AUTH_TOKEN_EXPIRED);
                    return;
                }
                case INVALID, TYPE_MISMATCH -> {
                    rejectWithErrorCode(request, response, ErrorCode.AUTH_TOKEN_INVALID);
                    return;
                }
                case VALID -> {
                    // continue below
                }
            }

            String username = jwtUtil.getUsername(accessToken);
            String role = jwtUtil.getRole(accessToken);

            UserEntity user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                log.warn("[JWTFilter] 토큰의 user를 DB에서 찾을 수 없음: {}", username);
                rejectWithErrorCode(request, response, ErrorCode.AUTH_USER_NOT_FOUND);
                return;
            }

            List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
            Authentication auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("[JWTFilter] 인증 성공 - {}", username);
            filterChain.doFilter(request, response);

        } catch (BusinessException be) {
            log.warn("[JWTFilter] 비즈니스 예외 - {}", be.getErrorCode());
            rejectWithErrorCode(request, response, be.getErrorCode());
        } catch (Exception e) {
            log.error("[JWTFilter] 검증 중 예외 발생", e);
            rejectWithErrorCode(request, response, ErrorCode.UNAUTHORIZED);
        }
    }

    private void rejectWithErrorCode(HttpServletRequest request,
                                     HttpServletResponse response,
                                     ErrorCode errorCode) throws IOException {
        request.setAttribute(JwtAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE, errorCode);
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response,
                new BadCredentialsException(errorCode.getMessage()));
    }

    private String extractAccessTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (ACCESS_COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                return (value == null || value.isBlank()) ? null : value;
            }
        }
        return null;
    }

    private boolean isExcludedPath(String uri) {
        return WhitelistPath.PATHS.stream().anyMatch(uri::contains);
    }
}
