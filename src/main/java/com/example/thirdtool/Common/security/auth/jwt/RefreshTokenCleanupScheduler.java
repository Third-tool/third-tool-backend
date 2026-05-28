package com.example.thirdtool.Common.security.auth.jwt;

import com.example.thirdtool.Common.security.auth.RefreshRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 만료된 Refresh Token을 주기적으로 DB에서 정리한다 (Story 2-4).
 *
 * - 매일 새벽 4시 실행 (KST 기준 cron)
 * - 기준: createdDate가 RT TTL(JwtProperties.refreshTokenTtl)보다 오래된 row
 * - local 프로파일에서는 비활성화 (운영/스테이징만 활성화)
 *
 * 삭제 중 DB 오류가 발생해도 다음 날 재시도되도록 예외는 catch 후 로그만 남긴다.
 */
@Slf4j
@Component
@Profile({"dev", "prod"})  // local 프로파일은 비활성. dev는 통합 테스트 환경 포함.
public class RefreshTokenCleanupScheduler {

    private final RefreshRepository refreshRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenCleanupScheduler(RefreshRepository refreshRepository, JwtProperties jwtProperties) {
        this.refreshRepository = refreshRepository;
        this.jwtProperties = jwtProperties;
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupExpiredRefreshTokens() {
        try {
            Duration ttl = jwtProperties.refreshTokenTtl();
            LocalDateTime threshold = LocalDateTime.now().minus(ttl);
            log.info("[RT-CLEANUP] 만료된 Refresh Token 삭제 시작 — threshold(createdDate < {})", threshold);

            refreshRepository.deleteByCreatedDateBefore(threshold);

            log.info("[RT-CLEANUP] 만료된 Refresh Token 삭제 완료");
        } catch (Exception e) {
            log.error("[RT-CLEANUP] 정리 중 오류 발생 — 다음 스케줄에 재시도. cause: {}", e.getMessage(), e);
        }
    }
}
