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
            // ✅ Rank 존재 시: Rank 점수 범위 기준으로 조회
            CardRank rank = cardRankRepository.findByUserIdAndName(dto.getUserId(), dto.getRankName().get())
                                              .orElseThrow(() -> new BusinessException(ErrorCode.RANK_NOT_FOUND));

            cards = cardRepository.findCardsByDeckAndModeAndScoreRange(
                    dto.getDeckId(),mode,
                    rank.getMinScore(),
                    rank.getMaxScore());
        } else {
            // ✅ Rank 없을 시: deck + mode 기준 전체 카드
            cards = cardRepository.findCardsByDeckAndMode(dto.getDeckId(), mode);
        }

        Long mainCardId = dto.getCardId()
                             .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        List<RecommendedCardDto> recommendedDtos = cards.stream()
                                                        .filter(card -> !card.getId().equals(mainCardId))
                                                        .limit(10)
                                                        .map(card -> {
                                                            String thumbnailUrl = card.getImages().stream()
                                                                                      .sorted(Comparator.comparing(CardImage::getSequence))
                                                                                      .map(CardImage::getImageUrl)
                                                                                      .findFirst()
                                                                                      .orElse(null);
                                                            return RecommendedCardDto.of(card.getId(), card.getQuestion(), card.getAnswer(), thumbnailUrl);
                                                        })
                                                        .collect(Collectors.toList());

        Collections.shuffle(recommendedDtos);
        return recommendedDtos;
    }


    /**
     * ✅ (2) 랜덤 버튼 클릭 시 학습 카드 1개 랜덤 선택 + 추천 리스트 반환
     * - score + rank + mode 기반 상위 10개 중 랜덤 1개 선택
     * 랜덤 버튼 눌렀을 때의  main과 레커맨드
     */
    @Transactional(readOnly = true)
    public CardMainResponseDto getRandomMainCard(CardLearningRequestDto dto) {

        Deck deck = deckRepository.findById(dto.getDeckId())
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        String rankName = dto.getRankName().orElse(null);

        // ✅ 1️⃣ rank 조건에 따른 카드 조회
        List<Card> candidates;

        if (rankName != null && !rankName.isBlank()) {
            // ✅ rank 존재 → 사용자별 rank 범위 가져오기
            CardRank rank = cardRankRepository.findByUserIdAndName(dto.getUserId(), rankName)
                                              .orElseThrow(() -> new BusinessException(ErrorCode.CARD_RANK_NOT_FOUND));

            // ✅ 점수 범위 기반 카드 조회
            candidates = cardRepository.findCardsByDeckAndModeAndScoreRange(
                    dto.getDeckId(),
                    dto.getMode(),
                    rank.getMinScore(),
                    rank.getMaxScore()
                                                                           );
        } else {
            // ✅ rankName이 없는 경우 → mode 기준으로만 조회
            candidates = cardRepository.findCardsByDeckAndMode(dto.getDeckId(), dto.getMode());
        }

        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        // ✅ 2️⃣ 랜덤 1개 선택
        Card mainCard = candidates.get(new Random().nextInt(candidates.size()));

        // ✅ 3️⃣ 이미지 조회 및 변환
        List<CardImageDto> mainImages = cardImageRepository.findByCardIdOrderBySequenceAsc(mainCard.getId())
                                                           .stream()
                                                           .map(CardImageDto::of)
                                                           .toList();

        CardImageGroupDto imageGroup = CardImageGroupDto.from(mainImages);

        return CardMainResponseDto.of(mainCard, imageGroup);
    }


    @Transactional(readOnly = true)
    public List<RecommendedCardDto> getRandomRecommendedCards(CardLearningRequestDto dto) {

        DeckMode mode = dto.getMode();
        Long deckId = dto.getDeckId();

        // ✅ Rank 존재 시 score 범위 기반으로 조회
        List<Card> candidates;
        if (dto.getRankName().isPresent()) {
            CardRank rank = cardRankRepository.findByUserIdAndName(dto.getUserId(), dto.getRankName().get())
                                              .orElseThrow(() -> new BusinessException(ErrorCode.RANK_NOT_FOUND));

            candidates = cardRepository.findCardsByDeckAndModeAndScoreRange(
                    deckId,
                    mode,
                    rank.getMinScore(),
                    rank.getMaxScore()
                                                                           );
        } else {
            // ✅ Rank 없을 시 전체 Mode 기반 조회
            candidates = cardRepository.findCardsByDeckAndMode(deckId, mode);
        }

        if (candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        // ✅ mainCard: 랜덤 1장 선택
        Card mainCard = candidates.get(new Random().nextInt(candidates.size()));

        // ✅ 추천 카드: mainCard 제외 후 DTO 변환
        List<RecommendedCardDto> recommendedDtos = candidates.stream()
                                                             .filter(card -> !card.getId().equals(mainCard.getId()))
                                                             .limit(10)
                                                             .map(card -> {
                                                                 String thumbnailUrl = card.getImages().stream()
                                                                                           .sorted(Comparator.comparing(CardImage::getSequence))
                                                                                           .map(CardImage::getImageUrl)
                                                                                           .findFirst()
                                                                                           .orElse(null);
                                                                 return RecommendedCardDto.of(card.getId(), card.getQuestion(), card.getAnswer(), thumbnailUrl);
                                                             })
                                                             .collect(Collectors.toList());

        // ✅ 무작위 순서 섞기
        Collections.shuffle(recommendedDtos);

        return recommendedDtos;
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