package com.example.thirdtool.LegacyCard.Card.application.service;

import com.example.thirdtool.LegacyCard.Card.domain.model.Card;
import com.example.thirdtool.LegacyCard.Card.domain.repository.CardImageRepository;
import com.example.thirdtool.LegacyCard.Card.domain.repository.CardRepository;
import com.example.thirdtool.LegacyCard.Card.presentation.dto.CardImageDto;
import com.example.thirdtool.LegacyCard.Card.presentation.dto.CardImageGroupDto;
import com.example.thirdtool.LegacyCard.Card.presentation.dto.response.CardSearchMainResponseDto;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchLearningService {

    private final CardRepository cardRepository;
    private final CardImageRepository cardImageRepository;
    private final DeckRepository deckRepository;

    /** 🔍 검색 메인 카드 (단일) */
    public CardSearchMainResponseDto getSearchMainCard(Long userId, Long cardId) {
        Card mainCard = cardRepository.findById(cardId)
                                      .filter(card -> card.getDeck().getUser().getId().equals(userId))
                                      .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        // 카드 이미지 로드 → 그룹화
        List<CardImageDto> images = cardImageRepository.findByCardIdOrderBySequenceAsc(cardId)
                                                       .stream().map(CardImageDto::of).toList();

        // ✅ 썸네일은 DTO.of 내부에서 ThumbnailPolicy 적용
        return CardSearchMainResponseDto.of(mainCard, CardImageGroupDto.from(images));
    }
}
