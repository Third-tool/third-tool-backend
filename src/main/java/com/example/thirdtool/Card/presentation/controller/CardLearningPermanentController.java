package com.example.thirdtool.Card.presentation.controller;

import com.example.thirdtool.Card.application.service.CardLearningPermanentService;
import com.example.thirdtool.Card.presentation.dto.RecommendedCardDto;
import com.example.thirdtool.Card.presentation.dto.request.CardLearningPermanentRequestDto;
import com.example.thirdtool.Card.presentation.dto.response.CardMainResponseDto;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards/learning/permanent")
public class CardLearningPermanentController {

    private final CardLearningPermanentService cardLearningPermanentService;

    /**
     * ✅ (1) 메인 카드 조회
     * 예: /api/cards/learning/permanent/{cardId}/main?deckId=1
     */
    @GetMapping("/{cardId}/main")
    public ResponseEntity<CardMainResponseDto> getPermanentMainCard(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long cardId,
            @RequestParam Long deckId) {

        CardLearningPermanentRequestDto request = CardLearningPermanentRequestDto.of(user.getId(), deckId, cardId);
        CardMainResponseDto response = cardLearningPermanentService.getPermanentMainCard(request);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ (2) 추천 카드 조회
     * 예: /api/cards/learning/permanent/{cardId}/recommendations?deckId=1
     */
    @GetMapping("/{cardId}/recommendations")
    public ResponseEntity<List<RecommendedCardDto>> getPermanentRecommendedCards(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long cardId,
            @RequestParam Long deckId) {

        CardLearningPermanentRequestDto request = CardLearningPermanentRequestDto.of(user.getId(), deckId, cardId);
        List<RecommendedCardDto> recommended = cardLearningPermanentService.getPermanentRecommendedCards(request);
        return ResponseEntity.ok(recommended);
    }

    /**
     * ✅ (1) 랜덤 메인 카드 조회
     * 예: /api/cards/learning/permanent/random/main?deckId=1
     */
    @GetMapping("/main")
    public ResponseEntity<CardMainResponseDto> getRandomPermanentMainCard(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam Long deckId) {

        CardLearningPermanentRequestDto request = CardLearningPermanentRequestDto.of(user.getId(), deckId);
        CardMainResponseDto response = cardLearningPermanentService.getRandomPermanentMainCard(request);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ (2) 랜덤 추천 카드 조회
     * 예: /api/cards/learning/permanent/random/recommendations?deckId=1
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendedCardDto>> getRandomPermanentRecommendations(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam Long deckId) {

        CardLearningPermanentRequestDto request = CardLearningPermanentRequestDto.of(user.getId(), deckId);
        List<RecommendedCardDto> recommended = cardLearningPermanentService.getRandomPermanentRecommendations(request);
        return ResponseEntity.ok(recommended);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getPermanentRemainingCount(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam Long deckId) {

        CardLearningPermanentRequestDto request = CardLearningPermanentRequestDto.of(user.getId(), deckId);
        long count = cardLearningPermanentService.getPermanentRemainingCardCount(request);

        return ResponseEntity.ok(Map.of("remainingCount", count));
    }



}