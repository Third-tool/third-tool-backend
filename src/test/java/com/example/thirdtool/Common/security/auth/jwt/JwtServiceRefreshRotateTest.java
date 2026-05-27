package com.example.thirdtool.Common.security.auth.jwt;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.Util.JWTUtil;
import com.example.thirdtool.Common.security.auth.RefreshRepository;
import com.example.thirdtool.Common.security.auth.dto.RefreshRequestDTO;
import com.example.thirdtool.Common.security.auth.token.TokenIssuer;
import com.example.thirdtool.Common.security.auth.token.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JwtService.refreshRotate — ErrorCode 세분화 (Story 2-1)")
class JwtServiceRefreshRotateTest {

    private static final String SECRET = "test-secret-key-must-be-32-bytes!!";

    private JwtService jwtService;
    private RefreshRepository refreshRepository;
    private JWTUtil jwtUtil;
    private TokenIssuer tokenIssuer;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(7));
        jwtUtil = new JWTUtil(props);
        refreshRepository = mock(RefreshRepository.class);
        tokenIssuer = mock(TokenIssuer.class);
        jwtService = new JwtService(refreshRepository, jwtUtil, tokenIssuer);
    }

    @Test
    @DisplayName("RT가 null이면 REFRESH_TOKEN_MISSING")
    void refresh_null_throwsMissing() {
        RefreshRequestDTO dto = new RefreshRequestDTO();

        assertThatThrownBy(() -> jwtService.refreshRotate(dto, new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_MISSING);
    }

    @Test
    @DisplayName("RT가 위조되면 REFRESH_TOKEN_INVALID")
    void refresh_garbage_throwsInvalid() {
        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken("garbage.token.string");

        assertThatThrownBy(() -> jwtService.refreshRotate(dto, new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    @DisplayName("RT가 만료되면 REFRESH_TOKEN_INVALID")
    void refresh_expired_throwsInvalid() {
        String expired = jwtUtil.createJWT("alice", "ROLE_USER", Duration.ZERO, TokenType.REFRESH);
        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken(expired);

        assertThatThrownBy(() -> jwtService.refreshRotate(dto, new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    @DisplayName("RT는 유효하지만 DB에 없으면 REFRESH_TOKEN_REUSED (이미 rotate된 RT 재사용 추정)")
    void refresh_dbNotFound_throwsReused() {
        String valid = jwtUtil.createJWT("alice", "ROLE_USER", Duration.ofDays(7), TokenType.REFRESH);
        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken(valid);

        when(refreshRepository.existsByRefresh(valid)).thenReturn(false);

        assertThatThrownBy(() -> jwtService.refreshRotate(dto, new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_REUSED);
    }

    @Test
    @DisplayName("RT 유효 + DB 존재 시 TokenIssuer.reissue가 호출된다")
    void refresh_valid_callsReissue() {
        String valid = jwtUtil.createJWT("alice", "ROLE_USER", Duration.ofDays(7), TokenType.REFRESH);
        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken(valid);

        when(refreshRepository.existsByRefresh(valid)).thenReturn(true);
        when(tokenIssuer.reissue(any(), any(), any())).thenReturn("new-rt-string");

        MockHttpServletResponse response = new MockHttpServletResponse();
        var result = jwtService.refreshRotate(dto, response);

        org.assertj.core.api.Assertions.assertThat(result.refreshToken()).isEqualTo("new-rt-string");
    }
}
