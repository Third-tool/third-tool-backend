package com.example.thirdtool.Common.security.filter;

import com.example.thirdtool.Common.Util.JWTUtil;
import com.example.thirdtool.Common.security.auth.jwt.JwtProperties;
import com.example.thirdtool.Common.security.auth.token.TokenType;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("JWTFilter — Cookie 기반 AT 추출 (Story 1-4)")
class JWTFilterTest {

    private static final String SECRET = "test-secret-key-must-be-32-bytes!!";

    private JWTUtil jwtUtil;
    private UserRepository userRepository;
    private JWTFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(7));
        jwtUtil = new JWTUtil(props);
        userRepository = mock(UserRepository.class);
        filter = new JWTFilter(userRepository, jwtUtil);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);

        request.setRequestURI("/cards");
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("쿠키 추출 흐름")
    class CookieExtraction {

        @Test
        @DisplayName("access_token 쿠키 존재 시 SecurityContext에 인증정보가 저장된다")
        void cookiePresent_validToken_authenticationSet() throws Exception {
            String token = jwtUtil.createJWT("alice", "ROLE_USER", Duration.ofMinutes(30), TokenType.ACCESS);
            request.setCookies(new Cookie("access_token", token));

            UserEntity user = UserEntity.ofLocal("alice", "encoded", "alice-nick", "alice@example.com");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            filter.doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .isInstanceOf(UserEntity.class);
            verify(chain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("쿠키 자체가 없으면 익명 요청으로 다음 필터로 진행")
        void noCookies_passesToNextFilter() throws Exception {
            // request.setCookies 호출 안 함 → cookies == null
            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(userRepository, never()).findByUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("access_token 키가 없는 쿠키 배열은 익명 요청 통과")
        void otherCookiesOnly_passesAsAnonymous() throws Exception {
            request.setCookies(new Cookie("other_cookie", "value"));

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        @DisplayName("Authorization 헤더가 있어도 쿠키가 없으면 익명 통과 (헤더 무시)")
        void authHeaderIgnored_whenNoCookie() throws Exception {
            String token = jwtUtil.createJWT("alice", "ROLE_USER", Duration.ofMinutes(30), TokenType.ACCESS);
            request.addHeader("Authorization", "Bearer " + token);

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(userRepository, never()).findByUsername(anyString());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("access_token 쿠키 값이 빈 문자열이면 익명 통과")
        void emptyCookieValue_passesAsAnonymous() throws Exception {
            request.setCookies(new Cookie("access_token", ""));

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
            verify(userRepository, never()).findByUsername(anyString());
        }
    }

    @Nested
    @DisplayName("JWT 검증 실패 흐름")
    class InvalidToken {

        @Test
        @DisplayName("위조된 JWT는 401 응답")
        void garbageToken_returns401() throws Exception {
            request.setCookies(new Cookie("access_token", "garbage.jwt.value"));

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(401);
            verify(chain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("만료된 JWT는 401 응답")
        void expiredToken_returns401() throws Exception {
            String expired = jwtUtil.createJWT("alice", "ROLE_USER", Duration.ZERO, TokenType.ACCESS);
            request.setCookies(new Cookie("access_token", expired));

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("화이트리스트 / health 경로")
    class WhitelistPaths {

        @Test
        @DisplayName("/health 경로는 쿠키 없어도 다음 필터 진행")
        void healthCheck_passes() throws Exception {
            request.setRequestURI("/health");

            filter.doFilter(request, response, chain);

            verify(chain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName(".php 경로는 즉시 404 차단")
        void phpPath_blocked404() throws Exception {
            request.setRequestURI("/something.php");

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(404);
            verify(chain, never()).doFilter(request, response);
        }
    }
}
