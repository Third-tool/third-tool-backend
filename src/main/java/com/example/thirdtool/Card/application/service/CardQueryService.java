package com.example.thirdtool.Card.application.service;


import com.example.thirdtool.Card.domain.model.Card;
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

    private final CardRepository cardRepository;

    /**
     * 카드 단건 조회.
     * 논리 삭제된 카드는 조회 불가.
     */
    public CardResponse.Detail findById(Long cardId) {
        return CardResponse.Detail.of(getActiveCard(cardId));
    }

    /**
     * 덱 내 카드 목록 조회.
     * 논리 삭제된 카드 제외.
     * mainNote 본문을 제외한 요약 구조로 반환.
     */
    public List<CardResponse.Summary> findAllByDeckId(Long deckId) {
        return cardRepository.findAllByDeckIdAndDeletedFalse(deckId)
                             .stream()
                             .map(CardResponse.Summary::of)
                             .toList();
    }

    // ─── 내부 공용 메서드 ─────────────────────────────────

    /**
     * 활성 카드 조회.
     * 존재하지 않거나 논리 삭제된 카드이면 예외.
     * CardCommandService에서 공통 사용.
     */
    public Card getActiveCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        if (card.isDeleted()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }
        return card;
    }
}