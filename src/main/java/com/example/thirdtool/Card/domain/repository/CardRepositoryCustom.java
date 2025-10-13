package com.example.thirdtool.Card.domain.repository;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.presentation.dto.CardInfoDto;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface CardRepositoryCustom {

    public Slice<CardInfoDto> findCardsByScoreRange(Long userId,
                                                    Long deckId,
                                                    DeckMode mode,
                                                    int minScore,
                                                    int maxScore,
                                                    Pageable pageable);

    /**
     * ✅ 5️⃣ 랭크 + 모드 기반으로 상위 N개 카드 조회 (3Day용)
     */
    List<Card> findTopNCardsByRankAndMode(Long userId, String rankName, DeckMode mode, int count);

    /**
     * ✅ 6️⃣ 특정 유저 + 랭크 + 모드 조건으로 남은 카드 개수 카운트
     */
    int countByRankAndMode(Long userId, String rankName, DeckMode mode);
}