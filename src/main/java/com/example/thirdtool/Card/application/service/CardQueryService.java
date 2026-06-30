package com.example.thirdtool.Card.application.service;


import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardRelationFinder;
import com.example.thirdtool.Card.domain.model.CardStatus;
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

    // ─── Tag 클릭 탐색 (Story 5-2) ────────────────────
    // 특정 tagId가 부착된 본인 카드를 ON_FIELD/ARCHIVE 모두 포함해 반환.
    // FE가 응답 DTO의 status 필드로 섹션 분리한다.

    public List<CardResponse.Summary> findByTag(Long tagId, Long userId) {
        return cardRepository.findByTagIdAndUserIdAndDeletedFalse(tagId, userId)
                             .stream()
                             .map(CardResponse.Summary::of)
                             .toList();
    }

    // ─── 축 스코프 카드 조회 (fix-deck-axis-visibility 0.0.2v Story 3·4) ───
    // 사용자의 axes에 연결된 Deck의 특정 status 카드를 단일 호출로 반환.
    // GET /learning-facade/axes/{axisId}/cards가 사용하며, today 집계와 동일 read-model
    // (CardRepository.findByUserIdAndAxisIdsAndStatus)을 공유한다.

    public List<CardResponse.Summary> findByAxisIds(Long userId, List<Long> axisIds, CardStatus status) {
        return cardRepository.findByUserIdAndAxisIdsAndStatus(userId, axisIds, status)
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

    // ─── ON_FIELD 학습 중 ARCHIVE 연결 후보 (Story 5-3) ───
    // 현재 카드의 Tag를 가진 본인의 ARCHIVE 카드만 필터링.
    // 공통 Tag 수 내림차순은 CardRelationFinder가 in-memory로 처리.

    public List<CardResponse.RelatedCard> findArchiveRelated(Long cardId, Long userId) {
        Card currentCard = findActiveCard(cardId);

        List<Long> tagIds = currentCard.getCardTags().stream()
                                       .map(ct -> ct.getTag().getId())
                                       .toList();

        // Tag가 없으면 섹션 미표시 — 빈 리스트 즉시 반환 (Spec 엣지 케이스)
        if (tagIds.isEmpty()) return List.of();

        List<Card> archivedCandidates =
                cardRepository.findArchivedBySharedTagIdsAndUserId(tagIds, cardId, userId);
        List<RelatedCardCandidate> candidates =
                cardRelationFinder.findCandidates(currentCard, archivedCandidates);

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