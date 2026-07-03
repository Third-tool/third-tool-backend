package com.example.thirdtool.Review.application;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.domain.model.SoftScheduleState;
import com.example.thirdtool.Card.domain.model.SoftScheduleTemplate;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeQueryService;
import com.example.thirdtool.Review.domain.model.ReviewSession;
import com.example.thirdtool.Review.domain.model.StateRecommendationDistributor;
import com.example.thirdtool.Review.infrastructure.ReviewSessionRepository;
import com.example.thirdtool.Review.infrastructure.dto.ReviewSessionSearchCondition;
import com.example.thirdtool.Review.presentation.dto.ReviewResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ReviewQueryService.
 *
 * <p>Story-CARD-E2-S2-4 — OnFieldBudget 폐기에 따라 `isLastView` 판정이 사라진다.
 * API 필드 호환을 위해 응답 DTO에는 남지만 항상 {@code false}로 반환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryService {

    private final ReviewSessionRepository reviewSessionRepository;
    private final UserScheduleQueryService userScheduleQueryService;
    private final CardRepository cardRepository;
    private final StateRecommendationDistributor stateRecommendationDistributor;
    private final LearningFacadeQueryService learningFacadeQueryService;

    // ─── 1. 세션 단건 조회 ────────────────────────────────
    public ReviewResponse.SessionDetail findById(Long sessionId, UserEntity user) {
        ReviewSession session = getSessionByOwner(sessionId, user);
        return ReviewResponse.SessionDetail.of(session, false);
    }


    // ─── 2. 세션 목록 조회 ────────────────────────────────
    /**
     * 세션 목록 조회.
     * deckId 지정 시 해당 덱의 세션만 반환, 미지정 시 전체 반환.
     * startedAt 내림차순 정렬은 QueryDSL에서 처리한다.
     */
    public List<ReviewResponse.SessionSummary> searchSessions(Long deckId, UserEntity user) {
        ReviewSessionSearchCondition condition = ReviewSessionSearchCondition.builder()
                                                                             .userId(user.getId())
                                                                             .deckId(deckId)
                                                                             .build();

        return reviewSessionRepository.searchSessions(condition)
                                      .stream()
                                      .map(ReviewResponse.SessionSummary::of)
                                      .toList();
    }

    // ─── 내부 공용 메서드 ─────────────────────────────────
    public ReviewSession getSessionByOwner(Long sessionId, UserEntity user) {
        // 세션 존재 여부: 없으면 REVIEW001
        ReviewSession session = reviewSessionRepository.findById(sessionId)
                                                       .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_SESSION_NOT_FOUND));

        // 소유자 검증: 본인 세션이 아니면 REVIEW005
        if (!session.isOwner(user.getId())) {
            throw new BusinessException(ErrorCode.REVIEW_SESSION_FORBIDDEN);
        }

        return session;
    }

    // ─── 3. 오늘의 학습 후보 (Story 6-1) ─────────────────

    public ReviewResponse.TodayCandidates getTodayCandidates(UserEntity user) {
        return collectToday(user, userScheduleQueryService.resolveDailyTarget(user.getId()));
    }

    /**
     * Story 6-3 — "+N장 추가 학습". 동일 풀에 대해 target만 다르게 적용해 분배를 재계산한다.
     * 호출자(FE)는 이미 표시된 카드 수 + 확장 N장을 합해 target으로 전달한다 (stateless).
     * 응답 {@code recommendedTotal}이 target보다 작으면 풀 소진 — "오늘 학습 완료" 안내 가능.
     */
    public ReviewResponse.TodayCandidates getTodayCandidatesWithTarget(UserEntity user, int target) {
        return collectToday(user, target);
    }

    private ReviewResponse.TodayCandidates collectToday(UserEntity user, int effectiveTarget) {
        Long userId = user.getId();
        SoftScheduleTemplate template = userScheduleQueryService.resolveSoftScheduleTemplate(userId);

        Duration minInterval = template.getIntervalSteps().get(0).minDuration();
        LocalDateTime threshold = LocalDateTime.now().minus(minInterval);

        List<Long> axisIds = learningFacadeQueryService.findAxisIdsByUserId(userId);
        List<Card> candidates = axisIds.isEmpty()
                ? cardRepository.findOnFieldEligibleByUserId(userId, threshold)
                : cardRepository.findByUserIdAndAxisIdsAndStatus(userId, axisIds, CardStatus.ON_FIELD);

        Map<SoftScheduleState, List<ReviewResponse.TodayCandidates.CandidateItem>> byState =
                candidates.stream()
                          .map(card -> Map.entry(template.resolveState(card), card))
                          .filter(entry -> entry.getKey() != SoftScheduleState.NOT_YET)
                          .collect(Collectors.groupingBy(
                                  Map.Entry::getKey,
                                  () -> new EnumMap<>(SoftScheduleState.class),
                                  Collectors.mapping(
                                          entry -> ReviewResponse.TodayCandidates.CandidateItem.of(entry.getValue()),
                                          Collectors.toList()
                                  )
                          ));

        int total = byState.values().stream().mapToInt(List::size).sum();

        int dailyTarget = userScheduleQueryService.resolveDailyTarget(userId);

        Map<SoftScheduleState, Integer> poolSizes = new EnumMap<>(SoftScheduleState.class);
        byState.forEach((state, list) -> poolSizes.put(state, list.size()));
        Map<SoftScheduleState, Integer> recommendedByState =
                stateRecommendationDistributor.distribute(poolSizes, effectiveTarget);
        int recommendedTotal = recommendedByState.values().stream().mapToInt(Integer::intValue).sum();

        return new ReviewResponse.TodayCandidates(
                total, dailyTarget, recommendedTotal, recommendedByState, byState
        );
    }
}
