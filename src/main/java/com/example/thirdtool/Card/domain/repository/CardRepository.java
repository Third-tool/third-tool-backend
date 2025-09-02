package com.example.thirdtool.Card.domain.repository;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long>,CardRepositoryCustom {

    List<Card> findByDeckIdAndMode(Long deckId, DeckMode mode);

    List<Card> findByDeckId(Long deckId);

    // ✅ 덱 ID와 모드를 기반으로 점수가 낮은 상위 N개의 카드를 가져오는 메서드
    List<Card> findByDeckIdAndModeOrderByScoreAsc(Long deckId, DeckMode mode, Pageable pageable);

}
