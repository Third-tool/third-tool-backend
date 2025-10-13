package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.repository.CardImageRepository;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Card.presentation.dto.CardImageDto;
import com.example.thirdtool.Card.presentation.dto.request.CardLearningPermanentRequestDto;
import com.example.thirdtool.Card.presentation.dto.request.CardLearningRequestDto;
import com.example.thirdtool.Card.presentation.dto.response.CardLearningPermanentResponseDto;
import com.example.thirdtool.Card.presentation.dto.response.CardLearningResponseDto;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardLearningService {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final CardImageRepository cardImageRepository;

    /**
     * ✅ (1) cardId로 특정 카드 학습 페이지 진입
     * - mainCard: 현재 학습할 카드
     * - rightPanel: score + rank + mode 기반 추천 10개
     */
    @Transactional(readOnly = true)
    public CardLearningResponseDto getLearningPageByCardId(Long userId,
                                                           Long deckId,
                                                           Long cardId,
                                                           DeckMode mode,
                                                           String rankName) {

        // ✅ 덱 존재 확인
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));
        deck.updateLastAccessed();

        // ✅ 메인 카드 조회
        Card mainCard = cardRepository.findById(cardId)
                                      .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        // ✅ 메인 카드 이미지 조회 (질문/답변 포함)
        List<CardImageDto> mainImages = cardImageRepository.findByCardIdOrderBySequenceAsc(cardId).stream()
                                                           .map(img -> new CardImageDto(img.getId(), img.getImageUrl(), img.getImageType(), img.getSequence()))
                                                           .toList();

        // ✅ 추천 카드 10개 (mainCard 제외)
        List<Card> recommended = cardRepository.findTopNCardsByRankAndMode(userId, rankName, mode, 10).stream()
                                               .filter(card -> !card.getId().equals(mainCard.getId()))
                                               .toList();

        Collections.shuffle(recommended);

        // ✅ 남은 카드 개수
        int totalRemaining = cardRepository.countByRankAndMode(userId, rankName, mode);

        // ✅ mainCard + recommendedCards + totalRemaining 반환
        return CardLearningResponseDto.of(mainCard, recommended, totalRemaining, mainImages);
    }

    /**
     * ✅ (2) 랜덤 버튼 클릭 시 학습 카드 1개 랜덤 선택 + 추천 리스트 반환
     * - score + rank + mode 기반 상위 10개 중 랜덤 1개 선택
     */
    @Transactional(readOnly = true)
    public CardLearningResponseDto getRandomLearningPage(CardLearningRequestDto dto) {
        Deck deck = deckRepository.findById(dto.getDeckId())
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        deck.updateLastAccessed();

        // 1️⃣ 점수 낮은 순으로 상위 10개 조회
        List<Card> topTen = cardRepository.findTopNCardsByRankAndMode(
                dto.getUserId(), dto.getRankName(), dto.getMode(), 10);

        if (topTen.isEmpty()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        // 2️⃣ 10개 중 랜덤 1개 선택
        Card mainCard = topTen.get(new Random().nextInt(topTen.size()));

        // ✅ 4️⃣ mainCard 이미지 조회
        List<CardImageDto> mainImages = cardImageRepository.findByCardIdOrderBySequenceAsc(mainCard.getId()).stream()
                                                           .map(img -> new CardImageDto(
                                                                   img.getId(),
                                                                   img.getImageUrl(),
                                                                   img.getImageType(),
                                                                   img.getSequence()
                                                           ))
                                                           .toList();

        // 3️⃣ 추천 카드 (mainCard 제외하고 다시 10개)
        List<Card> recommended = cardRepository.findTopNCardsByRankAndMode(
                                                       dto.getUserId(),
                                                       dto.getRankName(),
                                                       dto.getMode(),
                                                       10)
                                               .stream()
                                               .filter(card -> !card.getId().equals(mainCard.getId()))
                                               .toList();

        Collections.shuffle(recommended);

        // 4️⃣ 남은 카드 개수 계산
        int totalRemaining = cardRepository.countByRankAndMode(
                dto.getUserId(), dto.getRankName(), dto.getMode());

        // ✅ 7️⃣ mainCard + recommendedCards + images 포함 응답
        return CardLearningResponseDto.of(mainCard, recommended, totalRemaining, mainImages);
    }

    /**
     * ✅ (1) cardId 기반 학습 진입
     * - mainCard: 현재 학습 카드
     * - rightPanel: deckId + mode 기반 추천 10개 (랜덤)
     */
    @Transactional(readOnly = true)
    public CardLearningPermanentResponseDto preparePermanentLearning(CardLearningPermanentRequestDto dto) {
        Deck deck = deckRepository.findById(dto.getDeckId())
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        deck.updateLastAccessed();

        Long cardId = dto.getCardId()
                         .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        Card mainCard = cardRepository.findById(cardId)
                                      .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
        // ✅ mainCard 이미지 조회
        List<CardImageDto> mainImages = cardImageRepository.findByCardIdOrderBySequenceAsc(cardId).stream()
                                                           .map(img -> new CardImageDto(
                                                                   img.getId(),
                                                                   img.getImageUrl(),
                                                                   img.getImageType(),
                                                                   img.getSequence()
                                                           ))
                                                           .toList();
        List<Card> recommended = cardRepository.findTop10ByDeckIdAndMode(dto.getDeckId(), DeckMode.PERMANENT)
                                               .stream()
                                               .filter(card -> !card.getId().equals(mainCard.getId()))
                                               .toList();

        Collections.shuffle(recommended);
        // ✅ DTO 반환
        return CardLearningPermanentResponseDto.of(mainCard, recommended, mainImages);

    }

    /**
     * ✅ (2) 랜덤 학습 진입 (deckId + mode 기반)
     * - 10개 중 1개 랜덤 mainCard 선택
     */
    @Transactional(readOnly = true)
    public CardLearningPermanentResponseDto preparePermanentRandomLearning(CardLearningPermanentRequestDto dto) {
        Deck deck = deckRepository.findById(dto.getDeckId())
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        deck.updateLastAccessed();

        // ✅ PERMANENT 모드 카드 10개 조회
        List<Card> cards = cardRepository.findTop10ByDeckIdAndMode(dto.getDeckId(), DeckMode.PERMANENT);
        if (cards.isEmpty()) throw new BusinessException(ErrorCode.CARD_NOT_FOUND);

        // ✅ 랜덤 mainCard 선택
        Card mainCard = cards.get(new Random().nextInt(cards.size()));

        // ✅ mainCard 이미지 조회
        List<CardImageDto> mainImages = cardImageRepository.findByCardIdOrderBySequenceAsc(mainCard.getId()).stream()
                                                           .map(img -> new CardImageDto(
                                                                   img.getId(),
                                                                   img.getImageUrl(),
                                                                   img.getImageType(),
                                                                   img.getSequence()
                                                           ))
                                                           .toList();

        // ✅ 추천 카드
        List<Card> recommended = cards.stream()
                                      .filter(card -> !card.getId().equals(mainCard.getId()))
                                      .toList();

        Collections.shuffle(recommended);

        // ✅ DTO 반환
        return CardLearningPermanentResponseDto.of(mainCard, recommended, mainImages);
    }

}