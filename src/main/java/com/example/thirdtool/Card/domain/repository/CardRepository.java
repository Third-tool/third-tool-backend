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

public interface CardRepository extends JpaRepository<Card, Long>,CardRepositoryCustom {

    @Query("""
        select c from Card c
        where (:deckId is null or c.deck.id = :deckId)
          and (:mode is null or c.learningProfile.mode = :mode)
          and (:rankName is null or exists (
                select 1 from CardRank r
                where r.user.id = :userId
                  and c.learningProfile.score between r.minScore and r.maxScore
                  and r.name = :rankName
          ))
          and (:q is null or lower(c.question) like lower(concat('%', :q, '%'))
                       or lower(c.answer) like lower(concat('%', :q, '%')))
        """)
    Page<Card> search(
            @Param("userId") Long userId,
            @Param("deckId") Long deckId,
            @Param("mode") DeckMode mode,
            @Param("rankName") String rankName,
            @Param("q") String q,
            Pageable pageable
                     );

    @Query("select c from Card c " +
            "join c.learningProfile lp " +
            "where c.deck.id = :deckId and lp.mode = :mode")
    Slice<Card> findByDeckIdAndProfileMode(@Param("deckId") Long deckId,
                                           @Param("mode") DeckMode mode,
                                           Pageable pageable);

    Slice<Card>  findByDeckId(Long deckId,Pageable pageable);


    @Modifying
    @Query("update Card c set c.deck.id = :toDeckId where c.id in :cardIds")
    int bulkMove(@Param("cardIds") List<Long> cardIds, @Param("toDeckId") Long toDeckId);



    /**
     * ✅ rankName이 없는 경우 (단순 mode 기반)
     */
    @Query("""
        SELECT COUNT(c)
        FROM Card c
        WHERE c.deck.id = :deckId
          AND c.learningProfile.mode = :mode
    """)
    long countByDeckAndMode(@Param("deckId") Long deckId,
                            @Param("mode") DeckMode mode);

    /**
     * ✅ rankName이 있는 경우 (user별 점수 범위 조건 적용)
     */
    @Query("""
        SELECT COUNT(c)
        FROM Card c
        WHERE c.deck.id = :deckId
          AND c.learningProfile.mode = :mode
          AND c.learningProfile.score BETWEEN :minScore AND :maxScore
    """)
    long countByDeckAndModeAndScoreRange(@Param("deckId") Long deckId,
                                         @Param("mode") DeckMode mode,
                                         @Param("minScore") int minScore,
                                         @Param("maxScore") int maxScore);


    @Query("""
    SELECT c FROM Card c
    JOIN c.learningProfile lp
    WHERE c.deck.id = :deckId
      AND lp.mode = :mode
      AND lp.score BETWEEN :minScore AND :maxScore
""")
    List<Card> findCardsByDeckAndModeAndScoreRange(@Param("deckId") Long deckId,
                                                   @Param("mode") DeckMode mode,
                                                   @Param("minScore") int minScore,
                                                   @Param("maxScore") int maxScore);
    /**
     * ✅ (2) Rank 없이 Deck 전체에서 Mode로만 조회
     */
    @Query("""
        SELECT c
        FROM Card c
        JOIN c.deck d
        JOIN c.learningProfile lp
        WHERE d.id = :deckId
          AND lp.mode = :mode
        ORDER BY lp.score DESC
        """)
    List<Card> findCardsByDeckAndMode(
            @Param("deckId") Long deckId,
            @Param("mode") DeckMode mode
                                     );

}



