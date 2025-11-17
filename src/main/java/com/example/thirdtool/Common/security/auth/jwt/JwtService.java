package com.example.thirdtool.Common.security.auth.jwt;

import com.example.thirdtool.Common.security.auth.RefreshEntity;
import com.example.thirdtool.Common.security.auth.RefreshRepository;
import com.example.thirdtool.Common.security.auth.dto.JWTResponseDTO;
import com.example.thirdtool.Common.security.auth.dto.RefreshRequestDTO;
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

    public JwtService(RefreshRepository refreshRepository) {
        this.refreshRepository = refreshRepository;
    }

    @Transactional
    public JWTResponseDTO cookie2Header(
            HttpServletRequest request,
            HttpServletResponse response
                                       ) {

        // 쿠키 리스트
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new RuntimeException("쿠키가 존재하지 않습니다.");
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
            throw new RuntimeException("refreshToken 쿠키가 없습니다.");
        }

        // Refresh 토큰 검증
        Boolean isValid = JWTUtil.isValid(refreshToken, false);
        if (!isValid) {
            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
        }

        // 정보 추출
        String username = JWTUtil.getUsername(refreshToken);
        String role = JWTUtil.getRole(refreshToken);

        // 토큰 생성
        String newAccessToken = JWTUtil.createJWT(username, role, true);
        String newRefreshToken = JWTUtil.createJWT(username, role, false);

        // 기존 Refresh 토큰 DB 삭제 후 신규 추가
        RefreshEntity newRefreshEntity = RefreshEntity.builder()
                                                      .username(username)
                                                      .refresh(newRefreshToken)
                                                      .build();

        removeRefresh(refreshToken);
        refreshRepository.flush(); // 같은 트랜잭션 내부라 : 삭제 -> 생성 문제 해결
        refreshRepository.save(newRefreshEntity);

        // 기존 쿠키 제거
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(10);
        response.addCookie(refreshCookie);

        return new JWTResponseDTO(newAccessToken, newRefreshToken);
    }

    // Refresh 토큰으로 Access 토큰 재발급 로직 (Rotate 포함)
    // 로그로 임시 확인용
    @Transactional
    public JWTResponseDTO refreshRotate(RefreshRequestDTO dto) {
        String refreshToken = dto.getRefreshToken();
        log.info("[REFRESH-ROTATE] 전달받은 RefreshToken: {}", refreshToken);

        // Refresh 토큰 검증
        Boolean isValid = JWTUtil.isValid(refreshToken, false);
        log.info("[REFRESH-ROTATE] JWTUtil.isValid 결과: {}", isValid);

        if (!isValid) {
            log.error("[REFRESH-ROTATE] RefreshToken이 유효하지 않음 (JWT 파싱/만료 문제)");
            throw new RuntimeException("유효하지 않은 refreshToken입니다.-jwt가 이상하지롱");
        }

        // RefreshEntity 존재 확인 (화이트리스트)
        boolean exists = existsRefresh(refreshToken);
        log.info("[REFRESH-ROTATE] DB 화이트리스트 존재 여부: {}", exists);

        if (!exists) {
            log.error("[REFRESH-ROTATE] RefreshToken이 DB에 존재하지 않음");
            throw new RuntimeException("유효하지 않은 refreshToken입니다.-리프레쉬가 진짜 없지롱");
        }

        // 정보 추출
        String username = JWTUtil.getUsername(refreshToken);
        String role = JWTUtil.getRole(refreshToken);
        log.info("[REFRESH-ROTATE] 토큰에서 추출한 username={}, role={}", username, role);

        // 토큰 생성
        String newAccessToken = JWTUtil.createJWT(username, role, true);
        String newRefreshToken = JWTUtil.createJWT(username, role, false);
        log.info("[REFRESH-ROTATE] 새 AccessToken 생성 완료");
        log.info("[REFRESH-ROTATE] 새 RefreshToken 생성 완료");

        // ✅ 기존 username 기준으로 RefreshEntity 조회 (있으면 update, 없으면 insert)
        RefreshEntity entity = refreshRepository.findEntityByUsername(username)
                                                .orElse(RefreshEntity.builder().username(username).build());

        entity = RefreshEntity.builder()
                              .id(entity.getId()) // 있으면 그대로 유지
                              .username(username)
                              .refresh(newRefreshToken)
                              .build();

        refreshRepository.save(entity);
        log.info("[REFRESH-ROTATE] RefreshEntity 갱신 완료 (username 기반)");

        return new JWTResponseDTO(newAccessToken, newRefreshToken);
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
        return JWTUtil.getUsername(refreshToken);
    }
}
