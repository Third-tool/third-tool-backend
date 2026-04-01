package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.domain.model.Tag;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Card.infrastructure.persistence.TagRepository;
import com.example.thirdtool.Card.presentation.dto.CardRequest;
import com.example.thirdtool.Card.presentation.dto.CardResponse;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CardCommandService {

    private final CardRepository cardRepository;
    private final TagRepository tagRepository;
    private final DeckRepository deckRepository;

    // ─── 카드 생성 ─────────────────────────────────────

    public CardResponse.Create create(Long deckId, CardRequest.Create request) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> CardDomainException.of(
                                          ErrorCode.DECK_NOT_FOUND, "deckId=" + deckId));

        MainNote mainNote = MainNote.of(
                request.mainNote().textContent(),
                request.mainNote().imageUrl()
                                       );
        Summary summary = Summary.of(request.summary());

        List<Tag> tags = resolveTags(request.tags());

        Card card = Card.create(deck, mainNote, summary, request.keywords(), tags);
        cardRepository.save(card);
        return CardResponse.Create.of(card);
    }

    // ─── MainNote 수정 ─────────────────────────────────

    public CardResponse.UpdateMainNote updateMainNote(Long cardId, CardRequest.UpdateMainNote request) {
        Card card = findActiveCard(cardId);
        card.changeMainNote(request.textContent(), request.imageUrl());
        return CardResponse.UpdateMainNote.of(card);
    }

    // ─── Summary 수정 ──────────────────────────────────

    public CardResponse.UpdateSummary updateSummary(Long cardId, CardRequest.UpdateSummary request) {
        Card card = findActiveCard(cardId);
        card.changeSummary(request.summary());
        return CardResponse.UpdateSummary.of(card);
    }

    // ─── Keyword 전체 교체 ─────────────────────────────

    public CardResponse.Keywords replaceKeywords(Long cardId, CardRequest.ReplaceKeywords request) {
        Card card = findActiveCard(cardId);
        card.replaceKeywords(request.keywords());
        return CardResponse.Keywords.of(card);
    }

    // ───  Keyword 단건 추가 ─────────────────────────────

    public CardResponse.Keywords addKeyword(Long cardId, CardRequest.AddKeyword request) {
        Card card = findActiveCard(cardId);
        card.addKeyword(request.value());
        return CardResponse.Keywords.of(card);
    }

    // ─── Keyword 단건 제거 ─────────────────────────────

    public CardResponse.Keywords removeKeyword(Long cardId, Long keywordCueId) {
        Card card = findActiveCard(cardId);
        card.removeKeyword(keywordCueId);
        return CardResponse.Keywords.of(card);
    }

    // ─── 태그 단건 추가 ────────────────────────────────

    public CardResponse.Tags addTag(Long cardId, CardRequest.AddTag request) {
        Card card = findActiveCard(cardId);
        Tag  tag  = findOrCreateTag(request.value());
        card.addTag(tag);
        return CardResponse.Tags.of(card);
    }

    // ─── 태그 단건 제거 ───────────────────────────────

    public CardResponse.Tags removeTag(Long cardId, Long tagId) {
        Card card = findActiveCard(cardId);
        card.removeTag(tagId);
        return CardResponse.Tags.of(card);
    }

    // ─── 태그 전체 교체 ───────────────────────────────

    public CardResponse.Tags replaceTags(Long cardId, CardRequest.ReplaceTags request) {
        Card       card    = findActiveCard(cardId);
        List<Tag>  newTags = resolveTags(request.tags());
        card.replaceTags(newTags);
        return CardResponse.Tags.of(card);
    }

    // ─── 13. 카드 삭제 (Soft Delete) ──────────────────────

    public void softDelete(Long cardId) {
        Card card = findActiveCard(cardId);
        card.softDelete();
    }

    // ─── 내부 유틸 ────────────────────────────────────────

    private Card findActiveCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> CardDomainException.of(
                                          ErrorCode.CARD_NOT_FOUND, "cardId=" + cardId));
        if (card.isDeleted()) {
            throw CardDomainException.of(ErrorCode.CARD_NOT_FOUND, "cardId=" + cardId);
        }
        return card;
    }

    private List<Tag> resolveTags(List<String> tagValues) {
        if (tagValues == null || tagValues.isEmpty()) return Collections.emptyList();
        return tagValues.stream()
                        .map(this::findOrCreateTag)
                        .toList();
    }

    private Tag findOrCreateTag(String value) {
        return tagRepository.findByValue(value.trim())
                            .orElseGet(() -> tagRepository.save(Tag.of(value)));
    }

}