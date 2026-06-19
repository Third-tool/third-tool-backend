package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface CardJpaRepository extends JpaRepository<Card, Long>, CardRepositoryCustom {

    @Query("""
            SELECT DISTINCT c FROM Card c
            LEFT JOIN FETCH c.keywordCues
            WHERE c.id = :cardId
            """)
    Optional<Card> findByIdWithKeywords(@Param("cardId") Long cardId);


    List<Card> findAllByDeckIdAndDeletedFalse(Long deckId);


    List<Card> findAllByStatusAndDeletedFalse(CardStatus status);

    @Query("""
            SELECT DISTINCT c FROM Card c
            LEFT JOIN FETCH c.cardTags ct
            LEFT JOIN FETCH ct.tag
            WHERE ct.tag.id IN :tagIds
            AND c.id != :excludeCardId
            AND c.deleted = false
            """)
    List<Card> findBySharedTagIds(
            @Param("tagIds") List<Long> tagIds,
            @Param("excludeCardId") Long excludeCardId
                                 );

    /**
     * Story 5-2 — Tag 클릭 탐색 진입점.
     * <p>특정 tagId가 부착된 사용자 소유 활성 카드를 createdDate 내림차순으로 반환한다.
     * ON_FIELD / ARCHIVE 모두 포함 (FE에서 status 필드로 섹션 분리).
     */
    @Query("""
            SELECT DISTINCT c FROM Card c
            JOIN c.cardTags ct
            WHERE ct.tag.id = :tagId
              AND c.deck.user.id = :userId
              AND c.deleted = false
            ORDER BY c.createdDate DESC
            """)
    List<Card> findByTagIdAndUserIdAndDeletedFalse(
            @Param("tagId") Long tagId,
            @Param("userId") Long userId
                                                  );

    /**
     * Story 5-3 — ON_FIELD 학습 중 Tag 기반 ARCHIVE 연결 후보.
     * <p>전달받은 tagIds 중 하나 이상을 보유한 ARCHIVE 상태 사용자 카드를 자기 자신 제외하고 반환한다.
     * 공통 Tag 수 정렬은 도메인 서비스(CardRelationFinder)가 in-memory로 수행하므로
     * Repository는 후보 풀만 책임진다.
     */
    @Query("""
            SELECT DISTINCT c FROM Card c
            LEFT JOIN FETCH c.cardTags ct
            LEFT JOIN FETCH ct.tag
            WHERE ct.tag.id IN :tagIds
              AND c.id != :excludeCardId
              AND c.deck.user.id = :userId
              AND c.status = com.example.thirdtool.Card.domain.model.CardStatus.ARCHIVE
              AND c.deleted = false
            """)
    List<Card> findArchivedBySharedTagIdsAndUserId(
            @Param("tagIds") List<Long> tagIds,
            @Param("excludeCardId") Long excludeCardId,
            @Param("userId") Long userId
                                                  );

    /**
     * Story 5-1 — Tag 관리 화면의 "Tag 삭제" 액션.
     * 본인의 모든 활성 카드에서 해당 Tag 부착(CardTag row)을 일괄 해제한다.
     * Tag row 자체는 보존(시스템 전역 UNIQUE 자원, Open Question 5 v1 결정).
     *
     * <p>Aggregate 우회 — 본 메서드는 도메인 행위(`Card.removeTag`)를 N회 호출하는 대신
     * 영속 계층에서 직접 매핑 row만 제거한다. 카드 자체 상태 변경이 없으므로 안전.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM CardTag ct
            WHERE ct.tag.id = :tagId
              AND ct.card.deck.user.id = :userId
              AND ct.card.deleted = false
            """)
    int detachTagFromUserCards(@Param("userId") Long userId, @Param("tagId") Long tagId);

    /**
     * Story 6-1 — 사용자의 오늘 학습 후보 카드.
     * <p>본인 ON_FIELD 활성 카드 중 한 번도 노출되지 않았거나(lastViewedAt IS NULL)
     * 최소 간격을 통과한(lastViewedAt &le; :threshold) 카드 풀.
     * state별 분류는 도메인 {@link com.example.thirdtool.Card.domain.model.SoftScheduleTemplate}이 in-memory로 수행.
     * <p>Deck만 fetch join으로 함께 로딩(응답 매핑에 deckId/deckName이 필요). 카드 컬렉션(keywordCues,
     * cardTags)은 응답 DTO에 미포함이므로 fetch 미수행 — Hibernate MultipleBagFetchException 회피.
     */
    @Query("""
            SELECT c FROM Card c
            JOIN FETCH c.deck d
            WHERE d.user.id = :userId
              AND c.status = com.example.thirdtool.Card.domain.model.CardStatus.ON_FIELD
              AND c.deleted = false
              AND (c.lastViewedAt IS NULL OR c.lastViewedAt <= :threshold)
            """)
    List<Card> findOnFieldEligibleByUserId(
            @Param("userId") Long userId,
            @Param("threshold") java.time.LocalDateTime threshold
                                          );
}
