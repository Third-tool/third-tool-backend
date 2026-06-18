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
}
