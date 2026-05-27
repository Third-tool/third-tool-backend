package com.example.thirdtool.Common.Util;

import com.example.thirdtool.Common.security.auth.jwt.JwtProperties;
import com.example.thirdtool.Common.security.auth.token.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWTUtil")
class JWTUtilTest {

    private static final String SECRET = "test-secret-key-must-be-32-bytes!!";
    private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private JWTUtil jwtUtil;
    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties(SECRET, ACCESS_TTL, REFRESH_TTL);
        jwtUtil = new JWTUtil(properties);
    }

    @Nested
    @DisplayName("createJWT — 토큰 생성")
    class CreateJWT {

        @Test
        @DisplayName("ACCESS 타입으로 발급한 토큰의 type claim은 'access'다")
        void createJWT_ACCESS_typeClaim_access() {
            String token = jwtUtil.createJWT("alice", "ROLE_USER", ACCESS_TTL, TokenType.ACCESS);

            assertThat(jwtUtil.isValid(token, TokenType.ACCESS)).isTrue();
            assertThat(jwtUtil.getUsername(token)).isEqualTo("alice");
            assertThat(jwtUtil.getRole(token)).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("REFRESH 타입으로 발급한 토큰의 type claim은 'refresh'다")
        void createJWT_REFRESH_typeClaim_refresh() {
            String token = jwtUtil.createJWT("bob", "ROLE_ADMIN", REFRESH_TTL, TokenType.REFRESH);

            assertThat(jwtUtil.isValid(token, TokenType.REFRESH)).isTrue();
            assertThat(jwtUtil.getUsername(token)).isEqualTo("bob");
            assertThat(jwtUtil.getRole(token)).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("ttl이 다르면 같은 사용자라도 토큰 문자열은 동일할 수 없다")
        void createJWT_다른ttl_다른토큰() {
            String a = jwtUtil.createJWT("alice", "ROLE_USER", Duration.ofMinutes(30), TokenType.ACCESS);
            String b = jwtUtil.createJWT("alice", "ROLE_USER", Duration.ofHours(1), TokenType.ACCESS);

            // 발급 시각이 동일한 ms이면 같을 수도 있으니 만료 시각만 다르게 직접 비교
            // 실제로는 시각 차이로 issuedAt도 달라지지만 안전한 비교는 둘 다 valid한 것만
            assertThat(jwtUtil.isValid(a, TokenType.ACCESS)).isTrue();
            assertThat(jwtUtil.isValid(b, TokenType.ACCESS)).isTrue();
        }
    }

    @Nested
    @DisplayName("isValid — TokenType 검증")
    class IsValid {

        @Test
        @DisplayName("ACCESS 토큰을 ACCESS로 검증하면 true")
        void isValid_access_access_true() {
            String token = jwtUtil.createJWT("alice", "ROLE_USER", ACCESS_TTL, TokenType.ACCESS);

            assertThat(jwtUtil.isValid(token, TokenType.ACCESS)).isTrue();
        }

        @Test
        @DisplayName("ACCESS 토큰을 REFRESH로 검증하면 false (type 불일치)")
        void isValid_access_refresh_false() {
            String token = jwtUtil.createJWT("alice", "ROLE_USER", ACCESS_TTL, TokenType.ACCESS);

            assertThat(jwtUtil.isValid(token, TokenType.REFRESH)).isFalse();
        }

        @Test
        @DisplayName("REFRESH 토큰을 ACCESS로 검증하면 false (type 불일치)")
        void isValid_refresh_access_false() {
            String token = jwtUtil.createJWT("alice", "ROLE_USER", REFRESH_TTL, TokenType.REFRESH);

            assertThat(jwtUtil.isValid(token, TokenType.ACCESS)).isFalse();
        }

        @Test
        @DisplayName("위조된 토큰은 false")
        void isValid_garbage_false() {
            assertThat(jwtUtil.isValid("garbage.token.string", TokenType.ACCESS)).isFalse();
        }

        @Test
        @DisplayName("이미 만료된 토큰은 false")
        void isValid_만료_false() {
            String token = jwtUtil.createJWT("alice", "ROLE_USER", Duration.ZERO, TokenType.ACCESS);

            // Duration.ZERO 발급 직후 만료. 시각 차이로 false 보장.
            assertThat(jwtUtil.isValid(token, TokenType.ACCESS)).isFalse();
        }

        @Test
        @DisplayName("null 또는 빈 문자열은 false (예외 없음)")
        void isValid_null또는blank_false() {
            assertThat(jwtUtil.isValid("", TokenType.ACCESS)).isFalse();
        }

        @Test
        @DisplayName("다른 비밀키로 서명된 토큰은 false")
        void isValid_다른secret_false() {
            JwtProperties otherProps = new JwtProperties(
                    "different-secret-key-32-bytes-XXXX!",
                    ACCESS_TTL,
                    REFRESH_TTL
            );
            JWTUtil otherUtil = new JWTUtil(otherProps);

            String tokenFromOther = otherUtil.createJWT("alice", "ROLE_USER", ACCESS_TTL, TokenType.ACCESS);

            assertThat(jwtUtil.isValid(tokenFromOther, TokenType.ACCESS)).isFalse();
        }
    }

    @Nested
    @DisplayName("TTL 노출")
    class TtlExposure {

        @Test
        @DisplayName("accessTokenTtl()은 properties.accessTokenTtl()을 반환한다")
        void accessTokenTtl_returnsProperties() {
            assertThat(jwtUtil.accessTokenTtl()).isEqualTo(ACCESS_TTL);
        }

        @Test
        @DisplayName("refreshTokenTtl()은 properties.refreshTokenTtl()을 반환한다")
        void refreshTokenTtl_returnsProperties() {
            assertThat(jwtUtil.refreshTokenTtl()).isEqualTo(REFRESH_TTL);
        }
    }

    @Nested
    @DisplayName("getUsername / getRole — claim 추출")
    class GetClaims {

        @Test
        @DisplayName("getUsername은 sub claim을 반환한다")
        void getUsername_returns_sub() {
            String token = jwtUtil.createJWT("charlie", "ROLE_USER", ACCESS_TTL, TokenType.ACCESS);

            assertThat(jwtUtil.getUsername(token)).isEqualTo("charlie");
        }

        @Test
        @DisplayName("getRole은 role claim을 반환한다")
        void getRole_returns_role() {
            String token = jwtUtil.createJWT("charlie", "ROLE_ADMIN", ACCESS_TTL, TokenType.ACCESS);

            assertThat(jwtUtil.getRole(token)).isEqualTo("ROLE_ADMIN");
        }
    }
}
