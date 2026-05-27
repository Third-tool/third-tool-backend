package com.example.thirdtool.Common.security.auth.jwt;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.security.auth.RefreshEntity;
import com.example.thirdtool.Common.security.auth.RefreshRepository;
import com.example.thirdtool.Common.security.auth.dto.TokenResponse;
import com.example.thirdtool.Common.security.auth.dto.RefreshRequestDTO;
import com.example.thirdtool.Common.security.auth.token.TokenIssuer;
import com.example.thirdtool.Common.security.auth.token.TokenType;
import com.example.thirdtool.Common.Util.JWTUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
     * @deprecated Story 2-2에서 /jwt/exchange 엔드포인트와 함께 제거 예정.
     * 임시로 TokenIssuer.reissue를 위임해 JWTUtil.createJWT 직접 호출은 0건으로 유지.
     */
    @Deprecated
    @Transactional
    public TokenResponse cookie2Header(
            HttpServletRequest request,
            HttpServletResponse response
                                       ) {

        // 쿠키 리스트
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING);
        }

        // Refresh 토큰 획득
        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }

        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING);
        }

        // Refresh 토큰 검증
        if (!jwtUtil.isValid(refreshToken, TokenType.REFRESH)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 정보 추출 후 TokenIssuer 위임 (단일 진입점 보장)
        String username = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);
        String newRefreshToken = tokenIssuer.reissue(username, role, response);

        // 기존 RT 쿠키 제거
        Cookie expiredCookie = new Cookie("refreshToken", null);
        expiredCookie.setHttpOnly(true);
        expiredCookie.setSecure(false);
        expiredCookie.setPath("/");
        expiredCookie.setMaxAge(0);
        response.addCookie(expiredCookie);

        return new TokenResponse(newRefreshToken);
    }

    // Refresh 토큰으로 Access 토큰 재발급 로직 (Rotate 포함) — AT Cookie + RT 바디
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

        // 2. DB 화이트리스트 존재 확인
        if (!existsRefresh(refreshToken)) {
            // 토큰 자체는 유효한데 DB에 없으면 이미 rotate된 RT를 재사용한 것으로 간주
            log.warn("[REFRESH-ROTATE] RT 화이트리스트 미존재 — 재사용 시도 가능성");
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REUSED);
        }

        String username = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);
        log.debug("[REFRESH-ROTATE] RT 검증 통과 - username={}", username);

        String newRefreshToken = tokenIssuer.reissue(username, role, response);
        return new TokenResponse(newRefreshToken);
    }

    @Transactional
    public void addRefresh(String username, String refreshToken) {
        RefreshEntity entity = refreshRepository.findEntityByUsername(username)
                                                .orElseGet(() -> RefreshEntity.builder()
                                                                              .username(username)
                                                                              .build());

        // 기존 엔티티에 refreshToken만 갱신
        entity = RefreshEntity.builder()
                              .id(entity.getId()) // 기존 id 있으면 그대로 유지
                              .username(entity.getUsername())
                              .refresh(refreshToken)
                              .build();

        refreshRepository.save(entity); // JPA가 id 있으면 update, 없으면 insert
    }


    // JWT Refresh 존재 확인 메소드
    @Transactional(readOnly = true)
    public Boolean existsRefresh(String refreshToken) {
        return refreshRepository.existsByRefresh(refreshToken);
    }

    // JWT Refresh username 기반 확인 메소드
    public boolean existsRefreshByUsername(String username) {
        return refreshRepository.existsByUsername(username);
    }

    //JWT 리프레쉬 토큰 username 기반 존재하면 그거 가져오기 -> 토큰 줄거임
    public String getRefreshByUsername(String username) {
        return refreshRepository.findByUsername(username);
    }

    // JWT Refresh 토큰 삭제 메소드
    public void removeRefresh(String refreshToken) {
        refreshRepository.deleteByRefresh(refreshToken);
    }

    // 특정 유저 Refresh 토큰 모두 삭제 (탈퇴)
    public void removeRefreshUser(String username) {
        refreshRepository.deleteByUsername(username);
    }

    public String getUsername(String refreshToken) {
        return jwtUtil.getUsername(refreshToken);
    }
}
