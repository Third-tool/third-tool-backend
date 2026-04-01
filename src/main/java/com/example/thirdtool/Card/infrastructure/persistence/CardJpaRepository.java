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
}
