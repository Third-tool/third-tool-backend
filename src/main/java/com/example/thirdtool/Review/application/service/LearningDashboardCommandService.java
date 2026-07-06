package com.example.thirdtool.Review.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Review.domain.exception.ReviewSessionException;
import com.example.thirdtool.Review.domain.model.Recommendation;
import com.example.thirdtool.Review.domain.model.RecommendationAction;
import com.example.thirdtool.Review.infrastructure.RecommendationRepository;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * LearningDashboardCommandService — REV E3 · Story 3-5.
 *
 * <p>추천 accept/dismiss orchestration. accept 시 UserSchedule mode 변경 트리거.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LearningDashboardCommandService {

    private final RecommendationRepository recommendationRepository;
    private final UserScheduleCommandService userScheduleCommandService;

    /**
     * REV E3 · Story 3-5 — 추천 수락.
     *
     * <p>1. Recommendation 조회 · 소유권 검증<br>
     * 2. 이미 resolve됐으면 예외<br>
     * 3. UserScheduleCommandService.save(userId, toMode.maxDays()) 호출로 mode 변경<br>
     * 4. Recommendation.resolve(ACCEPTED, now) 이력 갱신
     */
    public void acceptRecommendation(Long userId, Long recommendationId) {
        Recommendation recommendation = loadOwned(userId, recommendationId);
        recommendation.resolve(RecommendationAction.ACCEPTED, LocalDateTime.now());

        // UserScheduleCommandService.save는 inputDays 기반 · toMode.maxDays()로 재배치.
        userScheduleCommandService.save(userId, recommendation.getToMode().maxDays());

        recommendationRepository.save(recommendation);
    }

    /**
     * REV E3 · Story 3-5 — 추천 거절. mode 변경 없이 이력만 갱신.
     */
    public void dismissRecommendation(Long userId, Long recommendationId) {
        Recommendation recommendation = loadOwned(userId, recommendationId);
        recommendation.resolve(RecommendationAction.DISMISSED, LocalDateTime.now());
        recommendationRepository.save(recommendation);
    }

    private Recommendation loadOwned(Long userId, Long recommendationId) {
        Recommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> ReviewSessionException.of(
                        ErrorCode.RECOMMENDATION_NOT_FOUND, "id=" + recommendationId));
        if (!rec.isOwner(userId)) {
            throw ReviewSessionException.of(ErrorCode.RECOMMENDATION_FORBIDDEN);
        }
        return rec;
    }
}
