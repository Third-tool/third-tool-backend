package com.example.thirdtool.Card.application.service;


import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardRelationFinder;
import com.example.thirdtool.Card.domain.model.RelatedCardCandidate;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Card.presentation.dto.CardResponse;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardQueryService {

    private final CardRepository    cardRepository;
    private final CardRelationFinder cardRelationFinder;

    // ─── 카드 단건 조회 ────────────────────────────────

    public CardResponse.Detail findById(Long cardId) {
        return CardResponse.Detail.of(findActiveCard(cardId));
    }

    // ─── 덱 내 카드 목록 조회 ─────────────────────────

    public List<CardResponse.Summary> findAllByDeckId(Long deckId) {
        return cardRepository.findAllByDeckIdAndDeletedFalse(deckId)
                             .stream()
                             .map(CardResponse.Summary::of)
                             .toList();
    }

    // ─── 관련 카드 후보 조회 ──────────────────────────

    public List<CardResponse.RelatedCard> findRelated(Long cardId) {
        Card currentCard = findActiveCard(cardId);

        List<Long> tagIds = currentCard.getCardTags().stream()
                                       .map(ct -> ct.getTag().getId())
                                       .toList();

        // 태그가 없으면 공유 카드가 없으므로 바로 빈 목록 반환
        if (tagIds.isEmpty()) return List.of();

        List<Card> taggedCards = cardRepository.findBySharedTagIds(tagIds, cardId);
        List<RelatedCardCandidate> candidates = cardRelationFinder.findCandidates(currentCard, taggedCards);

        return candidates.stream()
                         .map(CardResponse.RelatedCard::of)
                         .toList();
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
}