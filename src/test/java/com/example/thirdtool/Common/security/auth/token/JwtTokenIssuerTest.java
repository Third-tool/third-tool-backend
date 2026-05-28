package com.example.thirdtool.Common.security.auth.token;

import com.example.thirdtool.Common.Util.JWTUtil;
import com.example.thirdtool.Common.security.auth.RefreshEntity;
import com.example.thirdtool.Common.security.auth.RefreshRepository;
import com.example.thirdtool.Common.security.auth.jwt.JwtCookieProperties;
import com.example.thirdtool.Common.security.auth.jwt.JwtProperties;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("JwtTokenIssuer")
class JwtTokenIssuerTest {

    private static final String SECRET = "test-secret-key-must-be-32-bytes!!";

    private JWTUtil jwtUtil;
    private RefreshRepository refreshRepository;
    private JwtTokenIssuer issuer;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(7));
        jwtUtil = new JWTUtil(props);
        // Test default: prod-like (secure=true) — 별도 케이스에서 override
        JwtCookieProperties cookieProps = new JwtCookieProperties(
                "access_token", "/", true, true, "Strict"
        );
        refreshRepository = mock(RefreshRepository.class);
        issuer = new JwtTokenIssuer(jwtUtil, refreshRepository, cookieProps);
        response = new MockHttpServletResponse();

        when(refreshRepository.findEntityByUsername(any())).thenReturn(Optional.empty());
        when(refreshRepository.save(any(RefreshEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private JwtTokenIssuer issuerWithCookie(boolean secure, String sameSite) {
        JwtCookieProperties props = new JwtCookieProperties(
                "access_token", "/", true, secure, sameSite
        );
        return new JwtTokenIssuer(jwtUtil, refreshRepository, props);
    }

    @Nested
    @DisplayName("issue — 새 발급 흐름")
    class Issue {

        @Test
        @DisplayName("AT는 Set-Cookie 헤더로, RT는 반환값으로 전달된다")
        void issue_setsCookieAndReturnsRefresh() {
            UserEntity user = UserEntity.ofLocal("alice", "encoded", "alice-nick", "alice@example.com");

            String rt = issuer.issue(user, response);

            // RT는 반환값
            assertThat(rt).isNotBlank();
            assertThat(jwtUtil.isValid(rt, TokenType.REFRESH)).isTrue();

            // AT는 Set-Cookie 헤더에 들어가고 access_token 이름
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).isNotNull();
            assertThat(setCookie).contains("access_token=");
            assertThat(setCookie).contains("HttpOnly");
            assertThat(setCookie).contains("SameSite=Strict");
            assertThat(setCookie).contains("Path=/");
        }

        @Test
        @DisplayName("AT 토큰 자체는 유효한 ACCESS 타입이다")
        void issue_accessTokenIsValid() {
            UserEntity user = UserEntity.ofLocal("alice", "encoded", "alice-nick", "alice@example.com");

            issuer.issue(user, response);

            String setCookie = response.getHeader("Set-Cookie");
            String accessToken = extractCookieValue(setCookie, "access_token");
            assertThat(jwtUtil.isValid(accessToken, TokenType.ACCESS)).isTrue();
            assertThat(jwtUtil.getUsername(accessToken)).isEqualTo("alice");
            assertThat(jwtUtil.getRole(accessToken)).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("RT는 DB 화이트리스트에 자동 저장된다")
        void issue_persistsRefreshWhitelist() {
            UserEntity user = UserEntity.ofLocal("alice", "encoded", "alice-nick", "alice@example.com");

            issuer.issue(user, response);

            verify(refreshRepository, times(1)).save(any(RefreshEntity.class));
        }
    }

    @Nested
    @DisplayName("쿠키 보안 속성 프로파일 분기 (Story 1-3)")
    class CookieAttributes {

        @Test
        @DisplayName("prod 프로파일(secure=true) 발급 시 Set-Cookie에 Secure 포함")
        void issue_prodSecureTrue_setCookieHasSecure() {
            JwtTokenIssuer prodIssuer = issuerWithCookie(true, "Strict");
            UserEntity user = UserEntity.ofLocal("alice", "encoded", "alice-nick", "alice@example.com");

            prodIssuer.issue(user, response);

            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("Secure");
            assertThat(setCookie).contains("SameSite=Strict");
            assertThat(setCookie).contains("HttpOnly");
        }

        @Test
        @DisplayName("local/dev 프로파일(secure=false) 발급 시 Set-Cookie에 Secure 미포함")
        void issue_devSecureFalse_setCookieNoSecure() {
            JwtTokenIssuer devIssuer = issuerWithCookie(false, "Strict");
            UserEntity user = UserEntity.ofLocal("alice", "encoded", "alice-nick", "alice@example.com");

            devIssuer.issue(user, response);

            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).doesNotContain("Secure;").doesNotContain(" Secure");
            // SameSite=Strict / HttpOnly는 유지
            assertThat(setCookie).contains("SameSite=Strict");
            assertThat(setCookie).contains("HttpOnly");
        }

        @Test
        @DisplayName("SameSite=Lax 설정도 Set-Cookie에 반영된다")
        void issue_sameSiteLax_reflectedInSetCookie() {
            JwtTokenIssuer laxIssuer = issuerWithCookie(true, "Lax");
            UserEntity user = UserEntity.ofLocal("alice", "encoded", "alice-nick", "alice@example.com");

            laxIssuer.issue(user, response);

            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("SameSite=Lax");
        }
    }

    @Nested
    @DisplayName("AC4 — DB 화이트리스트 저장 실패 시 Cookie 발급도 롤백")
    class TransactionBoundary {

        @Test
        @DisplayName("DB save 실패 시 Set-Cookie 헤더가 응답에 추가되지 않는다")
        void issue_dbSaveFails_noCookieWritten() {
            when(refreshRepository.save(any(RefreshEntity.class)))
                    .thenThrow(new RuntimeException("DB down"));

            UserEntity user = UserEntity.ofLocal("alice", "encoded", "alice-nick", "alice@example.com");

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> issuer.issue(user, response)
            ).isInstanceOf(RuntimeException.class);

            assertThat(response.getHeader("Set-Cookie")).isNull();
        }
    }

    @Nested
    @DisplayName("reissue — refresh rotate 경로")
    class Reissue {

        @Test
        @DisplayName("username/role로 새 AT Cookie + RT 발급한다")
        void reissue_sameAsIssue() {
            String rt = issuer.reissue("bob", "ROLE_USER", response);

            assertThat(rt).isNotBlank();
            assertThat(jwtUtil.isValid(rt, TokenType.REFRESH)).isTrue();
            assertThat(jwtUtil.getUsername(rt)).isEqualTo("bob");

            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("access_token=");
        }

        @Test
        @DisplayName("reissue도 DB 화이트리스트 저장을 수행한다")
        void reissue_persistsWhitelist() {
            issuer.reissue("charlie", "ROLE_USER", response);

            verify(refreshRepository, times(1)).save(any(RefreshEntity.class));
        }
    }

    private static String extractCookieValue(String setCookie, String cookieName) {
        String prefix = cookieName + "=";
        int start = setCookie.indexOf(prefix);
        if (start < 0) return null;
        int valueStart = start + prefix.length();
        int valueEnd = setCookie.indexOf(';', valueStart);
        return valueEnd > 0 ? setCookie.substring(valueStart, valueEnd) : setCookie.substring(valueStart);
    }
}
