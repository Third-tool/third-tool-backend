package com.example.thirdtool.Card.domain.repository;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long>,CardRepositoryCustom {


    @Query("""
        SELECT c
        FROM Card c
        JOIN c.learningProfile lp
        WHERE c.deck.id = :deckId
          AND lp.mode = :mode
          AND c.deleted = false
    """)
    Slice<Card> findByDeckIdAndProfileMode(@Param("deckId") Long deckId,
                                           @Param("mode") DeckMode mode,
                                           Pageable pageable);

    /**
     * ✅ (3) 단순 덱 기반 카드 조회 (삭제 제외)
     */
    @Query("""
        SELECT c
        FROM Card c
        WHERE c.deck.id = :deckId
          AND c.deleted = false
    """)
    Slice<Card> findByDeckId(@Param("deckId") Long deckId, Pageable pageable);


    @Modifying
    @Query("UPDATE Card c SET c.deck.id = :toDeckId WHERE c.id IN :cardIds AND c.deleted = false")
    int bulkMove(@Param("cardIds") List<Long> cardIds, @Param("toDeckId") Long toDeckId);



    /**
     * ✅ (5) rankName이 없는 경우 (삭제 제외)
     */
    @Query("""
        SELECT COUNT(c)
        FROM Card c
        WHERE c.deck.id = :deckId
          AND c.learningProfile.mode = :mode
          AND c.deleted = false
    """)
    long countByDeckAndMode(@Param("deckId") Long deckId,
                            @Param("mode") DeckMode mode);


    /**
     * ✅ (6) rankName이 있는 경우 (삭제 제외)
     */
    @Query("""
        SELECT COUNT(c)
        FROM Card c
        WHERE c.deck.id = :deckId
          AND c.learningProfile.mode = :mode
          AND c.learningProfile.score BETWEEN :minScore AND :maxScore
          AND c.deleted = false
    """)
    long countByDeckAndModeAndScoreRange(@Param("deckId") Long deckId,
                                         @Param("mode") DeckMode mode,
                                         @Param("minScore") int minScore,
                                         @Param("maxScore") int maxScore);


    /**
     * ✅ (7) 점수 범위로 카드 조회 (삭제 제외)
     */
    @Query("""
        SELECT c
        FROM Card c
        JOIN c.learningProfile lp
        WHERE c.deck.id = :deckId
          AND lp.mode = :mode
          AND lp.score BETWEEN :minScore AND :maxScore
          AND c.deleted = false
    """)
    List<Card> findCardsByDeckAndModeAndScoreRange(@Param("deckId") Long deckId,
                                                   @Param("mode") DeckMode mode,
                                                   @Param("minScore") int minScore,
                                                   @Param("maxScore") int maxScore);
    /**
     * ✅ (8) Rank 없이 Deck 전체에서 Mode로만 조회 (삭제 제외)
     */
    @Query("""
        SELECT c
        FROM Card c
        JOIN c.deck d
        JOIN c.learningProfile lp
        WHERE d.id = :deckId
          AND lp.mode = :mode
          AND c.deleted = false
        ORDER BY lp.score DESC
    """)
    List<Card> findCardsByDeckAndMode(@Param("deckId") Long deckId,
                                      @Param("mode") DeckMode mode);


    /**
     * ✅ (9) 단순 덱 기반 전체 카드 조회 (삭제 제외)
     */
    @Query("""
        SELECT c
        FROM Card c
        WHERE c.deck.id = :deckId
          AND c.deleted = false
    """)
    List<Card> findByDeck_Id(@Param("deckId") Long deckId);

    /**
     * ✅ (10) 가장 최근에 생성된 카드 1개 (삭제 제외)
     * - DevDataInitializer용 (카드 생성 후 바로 참조)
     */
    @Query("""
        SELECT c
        FROM Card c
        WHERE c.deck.id = :deckId
          AND c.deleted = false
        ORDER BY c.id DESC
    """)
    Optional<Card> findLatestActiveCardByDeckId(@Param("deckId") Long deckId);
}



