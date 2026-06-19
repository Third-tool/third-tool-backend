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
     * 덱 내 활성 카드 목록 조회 (논리 삭제 제외).
     * CardQueryService.findAllByDeckId()에서 사용.
     */
    List<Card> findAllByDeckIdAndDeletedFalse(Long deckId);

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
     * ON_FIELD 만료 배치용 카드 조회.
     * 주어진 CardStatus를 가진 활성 카드 목록을 반환한다.
     */
    List<Card> findAllByStatus(CardStatus status);
    /**
     * 카드 검색 (QueryDSL Projection, 페이징).
     * 논리 삭제된 카드는 제외한다.
     */
    Page<CardSummaryRow> searchCards(CardSearchCondition condition, Pageable pageable);

}

