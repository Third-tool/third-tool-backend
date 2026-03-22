package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Card.presentation.dto.CardRequest;
import com.example.thirdtool.Card.presentation.dto.CardResponse;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.domain.model.Deck;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CardCommandService {

    private final CardRepository cardRepository;
    private final CardQueryService cardQueryService;
    private final DeckQueryService deckQueryService;

    /**
     * 카드 생성.
     * 덱 존재 여부 및 삭제 여부는 DeckQueryService.getActiveDeck()에서 검증.
     * 도메인 규칙 검증(키워드 최소 1개, MainNote 비어있음 등)은 도메인 내부에서 처리.
     */
    public CardResponse.Create create(Long deckId, CardRequest.Create request) {
        Deck deck = deckQueryService.getActiveDeck(deckId);

        Card card = Card.create(
                deck,
                MainNote.of(request.mainNote().textContent(), request.mainNote().imageUrl()),
                Summary.of(request.summary()),
                request.keywords()
                               );

        cardRepository.save(card);
        return CardResponse.Create.of(card);
    }

    /**
     * MainNote 수정.
     * textContent / imageUrl 둘 다 null이면 CARD_MAIN_NOTE_EMPTY 예외 (도메인 처리).
     */
    public CardResponse.UpdateMainNote updateMainNote(Long cardId, CardRequest.UpdateMainNote request) {
        Card card = cardQueryService.getActiveCard(cardId);
        card.changeMainNote(request.textContent(), request.imageUrl());
        return CardResponse.UpdateMainNote.of(card);
    }

    /**
     * Summary 수정.
     * 1~3문장 범위 검증은 Summary.of() 도메인 내부에서 처리.
     */
    public CardResponse.UpdateSummary updateSummary(Long cardId, CardRequest.UpdateSummary request) {
        Card card = cardQueryService.getActiveCard(cardId);
        card.changeSummary(request.summary());
        return CardResponse.UpdateSummary.of(card);
    }

    /**
     * Keyword 전체 교체.
     * 빈 리스트 전달 시 CARD_KEYWORD_MIN_REQUIRED 예외 (도메인 처리).
     */
    public CardResponse.Keywords replaceKeywords(Long cardId, CardRequest.ReplaceKeywords request) {
        Card card = cardQueryService.getActiveCard(cardId);
        card.replaceKeywords(request.keywords());
        return CardResponse.Keywords.of(card);
    }

    /**
     * Keyword 단건 추가.
     * 공백 키워드 전달 시 CARD_KEYWORD_BLANK 예외 (도메인 처리).
     */
    public CardResponse.Keywords addKeyword(Long cardId, CardRequest.AddKeyword request) {
        Card card = cardQueryService.getActiveCard(cardId);
        card.addKeyword(request.value());
        return CardResponse.Keywords.of(card);
    }

    /**
     * Keyword 단건 제거.
     * 마지막 키워드 제거 시도 시 CARD_KEYWORD_LAST_CANNOT_REMOVE 예외 (도메인 처리).
     * 존재하지 않는 keywordCueId 시 CARD_KEYWORD_NOT_FOUND 예외 (도메인 처리).
     */
    public CardResponse.Keywords removeKeyword(Long cardId, Long keywordCueId) {
        Card card = cardQueryService.getActiveCard(cardId);
        card.removeKeyword(keywordCueId);
        return CardResponse.Keywords.of(card);
    }

    /**
     * 카드 논리 삭제 (Soft Delete).
     * 이미 삭제된 카드는 CARD_NOT_FOUND 예외 (getActiveCard에서 처리).
     */
    public void softDelete(Long cardId) {
        Card card = cardQueryService.getActiveCard(cardId);
        card.softDelete();
    }

}