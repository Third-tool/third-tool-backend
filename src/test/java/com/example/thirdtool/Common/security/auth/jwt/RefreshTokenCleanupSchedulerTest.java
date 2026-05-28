package com.example.thirdtool.Common.security.auth.jwt;

import com.example.thirdtool.Common.security.auth.RefreshRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("RefreshTokenCleanupScheduler")
class RefreshTokenCleanupSchedulerTest {

    @Test
    @DisplayName("실행 시 RT TTL을 기준으로 deleteByCreatedDateBefore 호출")
    void cleanup_deletesRowsOlderThanTtl() {
        RefreshRepository repo = mock(RefreshRepository.class);
        JwtProperties props = new JwtProperties("test-secret-key-must-be-32-bytes!!",
                Duration.ofMinutes(30), Duration.ofDays(7));
        RefreshTokenCleanupScheduler scheduler = new RefreshTokenCleanupScheduler(repo, props);

        LocalDateTime expectedThresholdLower = LocalDateTime.now().minusDays(7).minusSeconds(2);
        LocalDateTime expectedThresholdUpper = LocalDateTime.now().minusDays(7).plusSeconds(2);

        scheduler.cleanupExpiredRefreshTokens();

        verify(repo, times(1)).deleteByCreatedDateBefore(argThat(threshold ->
                threshold.isAfter(expectedThresholdLower) && threshold.isBefore(expectedThresholdUpper)
        ));
    }

    @Test
    @DisplayName("삭제 중 DB 예외가 발생해도 스케줄러는 예외를 삼킨다 (다음 실행 보장)")
    void cleanup_dbError_swallowed() {
        RefreshRepository repo = mock(RefreshRepository.class);
        JwtProperties props = new JwtProperties("test-secret-key-must-be-32-bytes!!",
                Duration.ofMinutes(30), Duration.ofDays(7));
        RefreshTokenCleanupScheduler scheduler = new RefreshTokenCleanupScheduler(repo, props);

        doThrow(new RuntimeException("DB connection lost")).when(repo)
                .deleteByCreatedDateBefore(org.mockito.ArgumentMatchers.any());

        // 예외 전파되지 않아야 한다 (assertDoesNotThrow 의미)
        scheduler.cleanupExpiredRefreshTokens();

        verify(repo, times(1)).deleteByCreatedDateBefore(org.mockito.ArgumentMatchers.any());
    }
}
