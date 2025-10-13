package com.example.thirdtool.Common.security.filter;


import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.security.Util.JWTUtil;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

    private final UserRepository userRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        String requestUri = request.getRequestURI();

        log.info("[JWTFilter] 요청 URI: {}", requestUri);
        log.debug("[JWTFilter] Authorization Header: {}", authorization);

        // 1️⃣ Authorization 헤더 유무 확인
        if (authorization == null) {
            log.warn("[JWTFilter] Authorization 헤더 없음 → 다음 필터로 진행");
            filterChain.doFilter(request, response);
            return;
        }

        // 2️⃣ Bearer 형식 확인
        if (!authorization.startsWith("Bearer ")) {
            log.error("[JWTFilter] Authorization 헤더 형식 오류: {}", authorization);
            throw new ServletException("Invalid JWT token format (Bearer missing)");
        }

        // 3️⃣ Access Token 추출
        String accessToken = authorization.substring(7).trim(); // "Bearer " 이후 부분
        log.debug("[JWTFilter] 추출된 Access Token: {}", accessToken);

        // 4️⃣ JWT 유효성 검증
        try {
            if (JWTUtil.isValid(accessToken, true)) {
                String username = JWTUtil.getUsername(accessToken);
                String role = JWTUtil.getRole(accessToken);
                log.info("[JWTFilter] ✅ JWT username claim = {}", JWTUtil.getUsername(accessToken));
                log.info("[JWTFilter] ✅ JWT 유효함 → username={}, role={}", username, role);

                // 5️⃣ DB에서 사용자 확인
                UserEntity user = userRepository.findByUsername(username)
                                                .orElseThrow(() -> {
                                                    log.error("[JWTFilter] ❌ DB에서 사용자 없음: {}", username);
                                                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                                                });

                // 6️⃣ SecurityContext에 인증정보 저장
                List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
                Authentication auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.info("[JWTFilter] ✅ SecurityContextHolder 인증 성공 - {}", username);
                filterChain.doFilter(request, response);
                return;
            } else {
                log.warn("[JWTFilter] ❌ JWTUtil.isValid() → 토큰이 만료되었거나 유효하지 않음");
                writeUnauthorizedResponse(response, "토큰 만료 또는 유효하지 않은 토큰");
                return;
            }

        } catch (Exception e) {
            log.error("[JWTFilter] ❌ 예외 발생 during token validation: {}", e.getMessage(), e);
            writeUnauthorizedResponse(response, "JWT 검증 중 오류 발생: " + e.getMessage());
        }
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

}