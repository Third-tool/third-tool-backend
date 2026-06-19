package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
