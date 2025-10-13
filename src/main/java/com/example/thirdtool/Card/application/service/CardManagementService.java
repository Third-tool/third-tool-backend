package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.repository.CardImageRepository;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CardManagementService {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;

    // 단건 이동
    public void moveCard(Long cardId,
                         Long toDeckId) {
        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
        Deck toDeck = deckRepository.findById(toDeckId)
                                    .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        card.moveTo(toDeck);
    }

    // 단건 복제
    public Long copyCard(Long cardId,
                         Long toDeckId) {
        Card origin = cardRepository.findById(cardId)
                                    .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
        Deck toDeck = deckRepository.findById(toDeckId)
                                    .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        Card copy = origin.copyTo(toDeck);
        // 이미지 복제(정책에 따라): URL만 복제 or 새 업로드 X
        origin.getImages().forEach(img ->
                copy.addImage(CardImage.of(copy, img.getImageUrl(), img.getImageType(), img.getSequence())));
        cardRepository.save(copy);
        return copy.getId();
    }

    // 일괄 이동
    public int moveCards(List<Long> cardIds,
                         Long toDeckId) {
        if (cardIds == null || cardIds.isEmpty()) return 0;

        if (!deckRepository.existsById(toDeckId)) {
            throw new BusinessException(ErrorCode.DECK_NOT_FOUND);
        }
        return cardRepository.bulkMove(cardIds, toDeckId);
    }

    // 검색/필터
    @Transactional(readOnly = true)
    public Page<Card> search(Long userId,
                             Long deckId,
                             DeckMode mode,
                             String rankName,
                             String q,
                             Pageable pageable) {
        return cardRepository.search(userId, deckId, mode, rankName, q, pageable);
    }

}
