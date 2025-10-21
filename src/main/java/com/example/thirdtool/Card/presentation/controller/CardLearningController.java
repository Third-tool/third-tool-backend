package com.example.thirdtool.Card.presentation.controller;


import com.example.thirdtool.Card.application.service.CardLearningService;

import com.example.thirdtool.Card.presentation.dto.RecommendedCardDto;
import com.example.thirdtool.Card.presentation.dto.request.CardLearningRequestDto;
import com.example.thirdtool.Card.presentation.dto.response.CardMainResponseDto;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards")
public class CardLearningController {

    private final CardLearningService cardLearningService;


    /**
     * ✅ 1️⃣ 특정 카드 학습 페이지 진입
     * - 예: /api/cards/learning/{cardId}?deckId=1&mode=THREE_DAY&rankName=SILVER
     */
    // ✅ 메인 카드 조회 전용
    @GetMapping("/{cardId}/learning/main")
    public ResponseEntity<CardMainResponseDto> getMainCard(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long cardId,
            @RequestParam Long deckId,
            @RequestParam DeckMode mode,
            @RequestParam(required = false) String rankName) {

        CardLearningRequestDto request = CardLearningRequestDto.of(user.getId(), deckId, cardId, mode, rankName);
        CardMainResponseDto response = cardLearningService.getMainCardInfo(request);
        return ResponseEntity.ok(response);
    }

    // ✅ 추천 카드만 조회
    @GetMapping("/{cardId}/learning/recommendations")
    public ResponseEntity<List<RecommendedCardDto>> getRecommendedCards(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long cardId,
            @RequestParam Long deckId,
            @RequestParam DeckMode mode,
            @RequestParam(required = false) String rankName) {

        CardLearningRequestDto request = CardLearningRequestDto.of(user.getId(), deckId, cardId, mode, rankName);
        List<RecommendedCardDto> recommended = cardLearningService.getRecommendedCards(request);
        return ResponseEntity.ok(recommended);
    }


    /**
     * ✅ 2️⃣ 랜덤 학습 카드 진입
     * - 예: /api/cards/learning/random?deckId=1&mode=THREE_DAY&rankName=SILVER
     */
    /** ✅ (1) 랜덤 메인 카드 조회 */
    @GetMapping("/learning/random/main")
    public ResponseEntity<CardMainResponseDto> getRandomMainCard(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam Long deckId,
            @RequestParam DeckMode mode,
            @RequestParam String rankName) {

        CardLearningRequestDto request = CardLearningRequestDto.of(user.getId(), deckId, mode, rankName);
        CardMainResponseDto response = cardLearningService.getRandomMainCard(request);
        return ResponseEntity.ok(response);
    }


    /** ✅ (2) 랜덤 추천 카드 조회 */
    @GetMapping("/learning/random/recommendations")
    public ResponseEntity<List<RecommendedCardDto>> getRandomRecommendedCards(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam Long deckId,
            @RequestParam DeckMode mode,
            @RequestParam String rankName) {

        CardLearningRequestDto request = CardLearningRequestDto.of(user.getId(), deckId, mode, rankName);
        List<RecommendedCardDto> recommended = cardLearningService.getRandomRecommendedCards(request);
        return ResponseEntity.ok(recommended);
    }

    /**
     * ✅ 현재 mode + rank + deck 상태의 남은 카드 개수 조회
     * 예: /api/cards/learning/count?deckId=1&mode=THREE_DAY&rankName=SILVER
     */
    @GetMapping("/learning/count")
    public ResponseEntity<Map<String, Long>> getRemainingCardCount(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam Long deckId,
            @RequestParam DeckMode mode,
            @RequestParam(required = false) String rankName) {

        CardLearningRequestDto request = CardLearningRequestDto.of(user.getId(), deckId, mode, rankName);
        long count = cardLearningService.getRemainingCardCount(request);

        return ResponseEntity.ok(Map.of("remainingCount", count));
    }


}