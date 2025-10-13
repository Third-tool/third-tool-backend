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


    /**
     * ✅ 1️⃣ Deck + Mode 기준으로 점수가 낮은 순 정렬 후 상위 N개 조회 (3Day용)
     */
    @Query("""
        SELECT c FROM Card c
        JOIN c.learningProfile lp
        WHERE c.deck.id = :deckId
          AND lp.mode = :mode
        ORDER BY lp.score ASC
    """)
    List<Card> findTopNByDeckAndModeOrderByScoreAsc(@Param("deckId") Long deckId,
                                                    @Param("mode") DeckMode mode,
                                                    Pageable pageable);

    /**
     * ✅ 2️⃣ Permanent 모드 전용 추천 카드 10개 (deck 기반)
     */
    @Query("""
        SELECT c FROM Card c
        JOIN c.learningProfile lp
        WHERE c.deck.id = :deckId
          AND lp.mode = 'PERMANENT'
        ORDER BY RAND()
    """)
    List<Card> findTop10ByDeckIdAndMode(@Param("deckId") Long deckId,DeckMode mode);

    @Modifying
    @Query("update Card c set c.deck.id = :toDeckId where c.id in :cardIds")
    int bulkMove(@Param("cardIds") List<Long> cardIds, @Param("toDeckId") Long toDeckId);

    @Query("select count(c) from Card c where c.deck.id = :deckId")
    long countByDeckId(@Param("deckId") Long deckId);

}
