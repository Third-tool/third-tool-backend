package com.example.thirdtool.LearningFacade.application.dto;

/**
 * LearningFacade Query 입력 객체 묶음.
 *
 * <p>presentation 레이어 Request 누수 방지 + Service 메서드 시그니처를 Query record 1개로 통일.
 * 인자가 필요 없는 Query({@link GetActiveReasonOptions})도 명시적 marker record로 둔다.
 */
public final class LearningFacadeQuery {

    private LearningFacadeQuery() {}

    public record GetFacade(
            Long userId
    ) {}

    public record GetTopicRevisions(
            Long topicId
    ) {}

    public record GetTopicDeletions(
            Long axisId
    ) {}

    /**
     * 활성 수정 이유 선택지 목록 조회. 인자가 없지만 시그니처 통일을 위해 빈 record로 둔다.
     */
    public record GetActiveReasonOptions() {}

    /**
     * 축 스코프 카드 조회 — fix-deck-axis-visibility (0.0.2v) Fix-Story 4.
     * 소유권 검증(userId가 axisId의 축을 보유하는가) 후 CardQueryService.findByAxisIds에 위임.
     * status 기본값은 컨트롤러/서비스에서 ON_FIELD로 처리.
     */
    public record FindAxisCards(
            Long userId,
            Long axisId,
            com.example.thirdtool.Card.domain.model.CardStatus status
    ) {}
}
