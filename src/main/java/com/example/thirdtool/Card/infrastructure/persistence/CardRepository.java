package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.infrastructure.dto.CardSearchCondition;
import com.example.thirdtool.Card.infrastructure.dto.CardSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CardRepository {

    Card save(Card card);

    Optional<Card> findById(Long id);

    /**
     * @deprecated LT-E5-S5-2 (M5) — Deck BC 폐기와 함께 폐기. axis 스코프 조회로 이관.
     * 대체: {@link #findAllByAxisIdAndDeletedFalse(Long)}.
     */
    @Deprecated
    List<Card> findAllByDeckIdAndDeletedFalse(Long deckId);

    /**
     * LT-E5-S5-2 (M5) — 축 내 활성 카드 목록 조회 (논리 삭제 제외).
     * Deck 폐기 후 카드-축 직접 매핑 (axisId NOT NULL · V30 M4) 기반.
     * Review 세션 시작·Card 목록 조회 등에서 사용.
     */
    List<Card> findAllByAxisIdAndDeletedFalse(Long axisId);

    List<Card> findBySharedTagIds(List<Long> tagIds, Long excludeCardId);

    /**
     * Story 5-2 — Tag별 사용자 소유 카드 조회 (ON_FIELD/ARCHIVE 모두, 본인 카드만).
     * createdDate 내림차순. status 필드는 응답 DTO에 노출되어 FE가 섹션 분리.
     */
    List<Card> findByTagIdAndUserIdAndDeletedFalse(Long tagId, Long userId);

    /**
     * Story 5-3 — ON_FIELD 학습 중 Tag 기반 ARCHIVE 연결 후보 풀.
     * 전달받은 tagIds 중 하나 이상을 가진 ARCHIVE 상태 사용자 카드 (자기 자신 제외, deleted=false).
     * 공통 Tag 수 정렬은 CardRelationFinder가 in-memory로 수행한다.
     */
    List<Card> findArchivedBySharedTagIdsAndUserId(List<Long> tagIds, Long excludeCardId, Long userId);

    /**
     * Story 5-1 — Tag 관리 "삭제" 액션. 본인 활성 카드에서 해당 Tag 부착을 일괄 해제한다.
     * Tag row는 보존(시스템 전역 UNIQUE 자원). 반환값은 제거된 매핑 row 수.
     */
    int detachTagFromUserCards(Long userId, Long tagId);

    /**
     * Story 6-1 — 사용자의 오늘 학습 후보 카드 풀.
     * <p>본인 ON_FIELD 활성 카드 중 한 번도 노출되지 않았거나 threshold 이전에 노출된 카드를 반환한다.
     * 도메인 SoftScheduleTemplate가 in-memory에서 state별 분류·NOT_YET 제외를 책임진다.
     */
    List<Card> findOnFieldEligibleByUserId(Long userId, LocalDateTime threshold);

    /**
     * fix-deck-axis-visibility (0.0.2v) Story 3 — 축 스코프 Card 단일 read-model.
     * <p>사용자의 axes에 연결된 Deck의 특정 status 활성 카드를 반환한다. 축 카드 뷰와 today 집계가 공유한다.
     * eligibility(최소 간격) 재판정은 상위 호출자(Review)의 SoftScheduleTemplate가 in-memory로 수행하므로
     * 본 메서드는 threshold를 받지 않는다. axisIds null/빈 입력은 Adapter에서 빈 리스트로 단락.
     */
    List<Card> findByUserIdAndAxisIdsAndStatus(Long userId, List<Long> axisIds, CardStatus status);

    /**
     * ON_FIELD 만료 배치용 카드 조회.
     * 주어진 CardStatus를 가진 활성 카드 목록을 반환한다.
     */
    List<Card> findAllByStatus(CardStatus status);
    /**
     * 카드 검색 (QueryDSL Projection, 페이징).
     * 논리 삭제된 카드는 제외한다.
     */
    Page<CardSummaryRow> searchCards(CardSearchCondition condition, Pageable pageable);

    /**
     * Story-LT-E4-S4-4 — 축 스코프 카드 카운트 (Coverage 재계산 축 스코프).
     * 활성 카드만 계산 (deleted=false).
     */
    long countByAxisIdAndDeletedFalse(Long axisId);

    /**
     * Story-LT-E4-S4-4 — 축 스코프 특정 상태 카드 카운트 (Coverage 재계산 축 스코프).
     * 활성 카드만 계산 (deleted=false).
     */
    long countByAxisIdAndStatusAndDeletedFalse(Long axisId, CardStatus status);
}

