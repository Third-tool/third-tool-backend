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

    /** ✅ (1) 메인 카드 조회 */
    public CardMainResponseDto getPermanentMainCard(CardLearningPermanentRequestDto dto) {
        Deck deck = deckRepository.findById(dto.getDeckId())
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        Long cardId = dto.getCardId()
                         .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        Card mainCard = cardRepository.findById(cardId)
                                      .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        // ✅ 이미지 조회 + 그룹화
        List<CardImageDto> mainImages = cardImageRepository.findByCardIdOrderBySequenceAsc(cardId)
                                                           .stream()
                                                           .map(CardImageDto::of)
                                                           .toList();

        CardImageGroupDto imageGroup = CardImageGroupDto.from(mainImages);
        return CardMainResponseDto.of(mainCard, imageGroup);
    }

    @Transactional(readOnly = true)
    public List<RecommendedCardDto> getPermanentRecommendedCards(CardLearningPermanentRequestDto dto) {
        // ✅ 메인 카드 검증
        Long mainCardId = dto.getCardId()
                             .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        // ✅ PERMANENT 모드 카드 조회
        List<Card> cards = cardRepository.findCardsByDeckAndMode(dto.getDeckId(), DeckMode.PERMANENT);

        if (cards.isEmpty()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        // ✅ mainCard 제외 후 DTO 매핑
        List<RecommendedCardDto> recommendedDtos = cards.stream()
                                                        .filter(card -> !card.getId().equals(mainCardId))
                                                        .limit(10)
                                                        .map(this::mapToRecommendedDto)
                                                        .collect(Collectors.toList());

        // ✅ 순서 섞기
        Collections.shuffle(recommendedDtos);

        return recommendedDtos;
    }
    @Transactional(readOnly = true)
    public CardMainResponseDto getRandomPermanentMainCard(CardLearningPermanentRequestDto dto) {
        // ✅ 1. Deck 검증
        Deck deck = deckRepository.findById(dto.getDeckId())
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        // ✅ 2. PERMANENT 모드 카드 조회
        List<Card> cards = cardRepository.findCardsByDeckAndMode(dto.getDeckId(), DeckMode.PERMANENT);
        if (cards.isEmpty()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        // ✅ 3. 랜덤 메인 카드 선택
        Card mainCard = cards.get(new Random().nextInt(cards.size()));

        // ✅ 4. 이미지 그룹 구성
        List<CardImageDto> images = cardImageRepository.findByCardIdOrderBySequenceAsc(mainCard.getId())
                                                       .stream()
                                                       .map(CardImageDto::of)
                                                       .toList();

        CardImageGroupDto imageGroup = CardImageGroupDto.from(images);

        return CardMainResponseDto.of(mainCard, imageGroup);
    }


    @Transactional(readOnly = true)
    public List<RecommendedCardDto> getRandomPermanentRecommendations(CardLearningPermanentRequestDto dto) {
        // ✅ 1. PERMANENT 모드 카드 조회
        List<Card> cards = cardRepository.findCardsByDeckAndMode(dto.getDeckId(), DeckMode.PERMANENT);
        if (cards.isEmpty()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        // ✅ 2. mainCard 랜덤 선택
        Card mainCard = cards.get(new Random().nextInt(cards.size()));

        // ✅ 3. mainCard 제외 후 추천 카드 DTO 변환
        List<RecommendedCardDto> recommendedDtos = cards.stream()
                                                        .filter(card -> !card.getId().equals(mainCard.getId()))
                                                        .limit(10)
                                                        .map(this::mapToRecommendedDto)
                                                        .collect(Collectors.toList());

        // ✅ 4. 순서 섞기
        Collections.shuffle(recommendedDtos);

        return recommendedDtos;
    }

    @Transactional(readOnly = true)
    public long getPermanentRemainingCardCount(CardLearningPermanentRequestDto dto) {
        // deckId + mode(PERMANENT) 기준으로 카드 개수 조회
        return cardRepository.countByDeckAndMode(dto.getDeckId(), DeckMode.PERMANENT);
    }


    private RecommendedCardDto mapToRecommendedDto(Card card) {
        String thumbnailUrl = card.getImages().stream()
                                  .sorted(Comparator.comparing(CardImage::getSequence))
                                  .map(CardImage::getImageUrl)
                                  .findFirst()
                                  .orElse(null);

        return RecommendedCardDto.of(
                card.getId(),
                card.getQuestion(),
                card.getAnswer(),
                thumbnailUrl
                                    );
    }

}
