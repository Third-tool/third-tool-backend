package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.model.CardRank;
import com.example.thirdtool.Card.domain.repository.CardImageRepository;
import com.example.thirdtool.Card.domain.repository.CardRankRepository;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Card.presentation.dto.CardImageDto;
import com.example.thirdtool.Card.presentation.dto.CardImageGroupDto;
import com.example.thirdtool.Card.presentation.dto.RecommendedCardDto;
import com.example.thirdtool.Card.presentation.dto.request.CardLearningRequestDto;

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
public class CardLearningService {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final CardRankRepository cardRankRepository;
    private final CardImageRepository cardImageRepository;

    @Transactional(readOnly = true)
    public CardMainResponseDto getMainCardInfo(CardLearningRequestDto dto) {
        // ✅ 1️⃣ 메인 카드 조회 (cardId가 없는 경우 예외)
        Long cardId = dto.getCardId()
                         .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        Card mainCard = cardRepository.findById(cardId)
                                      .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        List<CardImageDto> allImages = cardImageRepository.findByCardIdOrderBySequenceAsc(mainCard.getId())
                                                          .stream()
                                                          .map(CardImageDto::of)
                                                          .toList();

        CardImageGroupDto imageGroup = CardImageGroupDto.from(allImages);

        return CardMainResponseDto.of(mainCard, imageGroup);
    }

    @Transactional(readOnly = true)
    public List<RecommendedCardDto> getRecommendedCards(CardLearningRequestDto dto) {
        DeckMode mode = dto.getMode();

        List<Card> cards;
        if (dto.getRankName().isPresent()) {
            CardRank rank = cardRankRepository.findByUserIdAndName(dto.getUserId(), dto.getRankName().get())
                                              .orElseThrow(() -> new BusinessException(ErrorCode.RANK_NOT_FOUND));

            cards = cardRepository.findCardsByDeckAndModeAndScoreRange(
                    dto.getDeckId(), mode, rank.getMinScore(), rank.getMaxScore()
                                                                      );
        } else {
            cards = cardRepository.findCardsByDeckAndMode(dto.getDeckId(), mode);
        }

        Long mainCardId = dto.getCardId()
                             .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        return cards.stream()
                    .filter(card -> !card.getId().equals(mainCardId))
                    .limit(10)
                    .map(RecommendedMappers::toRecommended) // ✅ 정책 적용
                    .collect(Collectors.toList());
    }




    @Transactional(readOnly = true)
    public CardMainResponseDto getRandomMainCard(CardLearningRequestDto dto) {
        Deck deck = deckRepository.findById(dto.getDeckId())
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        List<Card> candidates;
        if (dto.getRankName().isPresent()) {
            CardRank rank = cardRankRepository.findByUserIdAndName(dto.getUserId(), dto.getRankName().get())
                                              .orElseThrow(() -> new BusinessException(ErrorCode.CARD_RANK_NOT_FOUND));

            candidates = cardRepository.findCardsByDeckAndModeAndScoreRange(
                    dto.getDeckId(), dto.getMode(), rank.getMinScore(), rank.getMaxScore()
                                                                           );
        } else {
            candidates = cardRepository.findCardsByDeckAndMode(dto.getDeckId(), dto.getMode());
        }

        if (candidates.isEmpty()) throw new BusinessException(ErrorCode.CARD_NOT_FOUND);

        Card mainCard = candidates.get(new Random().nextInt(candidates.size()));

        List<CardImageDto> mainImages = cardImageRepository.findByCardIdOrderBySequenceAsc(mainCard.getId())
                                                           .stream().map(CardImageDto::of).toList();

        CardImageGroupDto imageGroup = CardImageGroupDto.from(mainImages);
        return CardMainResponseDto.of(mainCard, imageGroup); // ✅ 정책 적용
    }

    @Transactional(readOnly = true)
    public List<RecommendedCardDto> getRandomRecommendedCards(CardLearningRequestDto dto) {
        List<Card> candidates;
        if (dto.getRankName().isPresent()) {
            CardRank rank = cardRankRepository.findByUserIdAndName(dto.getUserId(), dto.getRankName().get())
                                              .orElseThrow(() -> new BusinessException(ErrorCode.RANK_NOT_FOUND));

            candidates = cardRepository.findCardsByDeckAndModeAndScoreRange(
                    dto.getDeckId(), dto.getMode(), rank.getMinScore(), rank.getMaxScore()
                                                                           );
        } else {
            candidates = cardRepository.findCardsByDeckAndMode(dto.getDeckId(), dto.getMode());
        }
        if (candidates.isEmpty()) throw new BusinessException(ErrorCode.CARD_NOT_FOUND);

        Card mainCard = candidates.get(new Random().nextInt(candidates.size()));

        return candidates.stream()
                         .filter(card -> !card.getId().equals(mainCard.getId()))
                         .limit(10)
                         .map(RecommendedMappers::toRecommended) // ✅ 정책 적용
                         .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getRemainingCardCount(CardLearningRequestDto dto) {
        String rankName = dto.getRankName().orElse(null);

        // ✅ (1) rank가 존재하지 않으면 단순 count
        if (rankName == null || rankName.isBlank()) {
            return cardRepository.countByDeckAndMode(dto.getDeckId(), dto.getMode());
        }

        // ✅ (2) rank가 존재하면 user의 rank 범위 가져오기
        CardRank rank = cardRankRepository.findByUserIdAndName(dto.getUserId(), rankName)
                                          .orElseThrow(() -> new BusinessException(ErrorCode.CARD_RANK_NOT_FOUND));

        // ✅ (3) 범위 기반 count
        return cardRepository.countByDeckAndModeAndScoreRange(
                dto.getDeckId(),
                dto.getMode(),
                rank.getMinScore(),
                rank.getMaxScore()
                                                             );
    }



}