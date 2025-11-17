package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.repository.CardImageRepository;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Card.presentation.dto.CardImageDto;
import com.example.thirdtool.Card.presentation.dto.CardImageGroupDto;
import com.example.thirdtool.Card.presentation.dto.RecommendedCardDto;
import com.example.thirdtool.Card.presentation.dto.request.CardLearningPermanentRequestDto;
import com.example.thirdtool.Card.presentation.dto.response.CardMainResponseDto;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.Util.mapper.RecommendedMappers;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardLearningPermanentService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardImageRepository cardImageRepository;

    public CardMainResponseDto getPermanentMainCard(CardLearningPermanentRequestDto dto) {
        Deck deck = deckRepository.findById(dto.getDeckId())
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        Long cardId = dto.getCardId()
                         .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        Card mainCard = cardRepository.findById(cardId)
                                      .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        List<CardImageDto> mainImages = cardImageRepository.findByCardIdOrderBySequenceAsc(cardId)
                                                           .stream().map(CardImageDto::of).toList();

        CardImageGroupDto imageGroup = CardImageGroupDto.from(mainImages);
        return CardMainResponseDto.of(mainCard, imageGroup); // ✅ 정책 적용
    }

    public List<RecommendedCardDto> getPermanentRecommendedCards(CardLearningPermanentRequestDto dto) {
        Long mainCardId = dto.getCardId()
                             .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        List<Card> cards = cardRepository.findCardsByDeckAndMode(dto.getDeckId(), DeckMode.PERMANENT);
        if (cards.isEmpty()) throw new BusinessException(ErrorCode.CARD_NOT_FOUND);

        List<RecommendedCardDto> out = cards.stream()
                                            .filter(c -> !c.getId().equals(mainCardId))
                                            .limit(10)
                                            .map(RecommendedMappers::toRecommended) // ✅ 정책 적용
                                            .collect(Collectors.toList());
        Collections.shuffle(out);
        return out;
    }

    public CardMainResponseDto getRandomPermanentMainCard(CardLearningPermanentRequestDto dto) {
        deckRepository.findById(dto.getDeckId())
                      .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        List<Card> cards = cardRepository.findCardsByDeckAndMode(dto.getDeckId(), DeckMode.PERMANENT);
        if (cards.isEmpty()) throw new BusinessException(ErrorCode.CARD_NOT_FOUND);

        Card mainCard = cards.get(new Random().nextInt(cards.size()));

        List<CardImageDto> images = cardImageRepository.findByCardIdOrderBySequenceAsc(mainCard.getId())
                                                       .stream().map(CardImageDto::of).toList();

        CardImageGroupDto imageGroup = CardImageGroupDto.from(images);
        return CardMainResponseDto.of(mainCard, imageGroup); // ✅ 정책 적용
    }

    public List<RecommendedCardDto> getRandomPermanentRecommendations(CardLearningPermanentRequestDto dto) {
        List<Card> cards = cardRepository.findCardsByDeckAndMode(dto.getDeckId(), DeckMode.PERMANENT);
        if (cards.isEmpty()) throw new BusinessException(ErrorCode.CARD_NOT_FOUND);

        Card mainCard = cards.get(new Random().nextInt(cards.size()));

        List<RecommendedCardDto> out = cards.stream()
                                            .filter(c -> !c.getId().equals(mainCard.getId()))
                                            .limit(10)
                                            .map(RecommendedMappers::toRecommended) // ✅ 정책 적용
                                            .collect(Collectors.toList());
        Collections.shuffle(out);
        return out;
    }

    @Transactional(readOnly = true)
    public long getPermanentRemainingCardCount(CardLearningPermanentRequestDto dto) {
        // deckId + mode(PERMANENT) 기준으로 카드 개수 조회
        return cardRepository.countByDeckAndMode(dto.getDeckId(), DeckMode.PERMANENT);
    }



}
