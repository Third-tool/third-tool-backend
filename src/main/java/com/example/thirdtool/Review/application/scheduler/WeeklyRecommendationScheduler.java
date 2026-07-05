package com.example.thirdtool.Review.application.scheduler;

import com.example.thirdtool.Review.application.service.NotificationService;
import com.example.thirdtool.Review.domain.model.NotificationType;
import com.example.thirdtool.Review.domain.model.Recommendation;
import com.example.thirdtool.Review.domain.service.RecommendationEngine;
import com.example.thirdtool.Review.infrastructure.RecommendationRepository;
import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfig;
import com.example.thirdtool.UserSchedule.infrastructure.persistence.UserScheduleConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * WeeklyRecommendationScheduler — REV E3 · Story 3-3.
 *
 * <p>매주 월요일 09:00 KST에 활성 사용자 순회 · RecommendationEngine 판정 · notification 발송.
 * 이미 이번 주에 발송된 사용자에겐 중복 발송 안 함 (같은 주 1회).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyRecommendationScheduler {

    private final UserScheduleConfigRepository userConfigRepository;
    private final RecommendationEngine engine;
    private final RecommendationRepository recommendationRepository;
    private final NotificationService notificationService;

    /**
     * REV E3 · Story 3-3 — 주간 추천 판정 cron.
     * <p>cron: 매주 월요일 09:00 KST · {@code "0 0 9 ? * MON"}.
     */
    @Scheduled(cron = "0 0 9 ? * MON", zone = "Asia/Seoul")
    @Transactional
    public void checkWeeklyRecommendations() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.minusDays(7);

        int total = 0;
        int suggested = 0;

        for (UserScheduleConfig config : userConfigRepository.findAll()) {
            total++;
            Long userId = config.getUserId();

            // 이미 이번 주에 발송된 사용자 skip
            if (recommendationRepository.findLatestByUserIdSince(userId, weekStart).isPresent()) {
                continue;
            }

            Optional<RecommendationEngine.Evaluation> eval = engine.evaluate(userId, today);
            if (eval.isEmpty()) continue;

            Recommendation saved = recommendationRepository.save(
                    Recommendation.of(userId, eval.get().type(), eval.get().fromMode(),
                            eval.get().toMode(), eval.get().reason(), now));

            String payload = String.format(
                    "{\"type\":\"%s\",\"fromMode\":\"%s\",\"toMode\":\"%s\",\"reason\":\"%s\"}",
                    eval.get().type().name(),
                    eval.get().fromMode().name(),
                    eval.get().toMode().name(),
                    escape(eval.get().reason()));

            NotificationType notificationType = switch (eval.get().type()) {
                case SUGGEST_DOWNGRADE -> NotificationType.SUGGEST_DOWNGRADE;
                case SUGGEST_UPGRADE -> NotificationType.SUGGEST_UPGRADE;
            };

            notificationService.send(userId, notificationType, payload, saved.getId());
            suggested++;
        }

        log.info("WeeklyRecommendationScheduler done. users={} suggested={}", total, suggested);
    }

    private static String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}
