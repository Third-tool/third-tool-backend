package com.example.thirdtool.Common.security.auth.jwt;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.security.auth.RefreshRepository;
import com.example.thirdtool.Common.security.auth.dto.RefreshRequestDTO;
import com.example.thirdtool.Common.security.auth.dto.TokenResponse;
import com.example.thirdtool.Common.security.auth.token.TokenIssuer;
import com.example.thirdtool.Common.security.auth.token.TokenType;
import com.example.thirdtool.Common.Util.JWTUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class JwtService {

    private final RefreshRepository refreshRepository;
    private final JWTUtil jwtUtil;
    private final TokenIssuer tokenIssuer;

    public JwtService(RefreshRepository refreshRepository, JWTUtil jwtUtil, TokenIssuer tokenIssuer) {
        this.refreshRepository = refreshRepository;
        this.jwtUtil = jwtUtil;
        this.tokenIssuer = tokenIssuer;
    }

    /**
     * Refresh Token으로 새 AT Cookie + 새 RT를 발급한다 (Rotate 포함).
     * 실패 케이스별 ErrorCode 분기 (Story 2-1).
     */
    @Transactional
    public TokenResponse refreshRotate(RefreshRequestDTO dto, HttpServletResponse response) {
        String refreshToken = dto.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING);
        }

        // 1. JWT 파싱/만료 검증
        if (!jwtUtil.isValid(refreshToken, TokenType.REFRESH)) {
            log.warn("[REFRESH-ROTATE] RT 파싱 또는 만료 실패");
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 2. DB 화이트리스트 존재 확인 — 이미 rotate된 RT 재사용 차단
        if (!refreshRepository.existsByRefresh(refreshToken)) {
            log.warn("[REFRESH-ROTATE] RT 화이트리스트 미존재 — 재사용 시도 가능성");
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REUSED);
        }

        String username = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);
        log.debug("[REFRESH-ROTATE] RT 검증 통과 - username={}", username);

        String newRefreshToken = tokenIssuer.reissue(username, role, response);
        return new TokenResponse(newRefreshToken);
    }

    /**
     * 특정 유저의 모든 Refresh Token을 삭제한다 (회원 탈퇴 등 cascade 정리).
     */
    @Transactional
    public void removeRefreshUser(String username) {
        refreshRepository.deleteByUsername(username);
    }
}
