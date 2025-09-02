package com.example.thirdtool.Card.domain.repository;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.presentation.dto.CardInfoDto;
import com.example.thirdtool.Deck.domain.model.DeckMode;

import java.util.List;

public interface CardRepositoryCustom {

    List<CardInfoDto> findCardsByRankWithQuerydsl(Long userId, Long deckId, CardRankType rankName, DeckMode mode); // ✅ deckId 추가

    // ✅ 랭크, 모드 기준으로 점수가 낮은 상위 N개의 카드를 조회하는 메서드
    List<Card> findTopNCardsByRankAndMode(Long userId, String rankName, DeckMode mode, int count);
}
