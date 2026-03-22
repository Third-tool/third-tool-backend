package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
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

    /**
     * 덱 내 활성 카드 목록 조회 — 논리 삭제 제외.
     * Spring Data JPA 네이밍 규칙으로 자동 구현.
     */
    List<Card> findAllByDeckIdAndDeletedFalse(Long deckId);

}
