package com.example.thirdtool.Card.presentation.controller;


import com.example.thirdtool.Card.application.service.CardLearningService;

import com.example.thirdtool.Card.presentation.dto.request.CardLearningPermanentRequestDto;
import com.example.thirdtool.Card.presentation.dto.request.CardLearningRequestDto;
import com.example.thirdtool.Card.presentation.dto.response.CardLearningPermanentResponseDto;
import com.example.thirdtool.Card.presentation.dto.response.CardLearningResponseDto;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping("/{cardId}/learning")
    public ResponseEntity<CardLearningResponseDto> getLearningPageByCardId(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long cardId,
            @RequestParam Long deckId,
            @RequestParam DeckMode mode,
            @RequestParam String rankName) {

        CardLearningResponseDto response = cardLearningService.getLearningPageByCardId(
                user.getId(), deckId, cardId, mode, rankName);

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ 2️⃣ 랜덤 학습 카드 진입
     * - 예: /api/cards/learning/random?deckId=1&mode=THREE_DAY&rankName=SILVER
     */
    @GetMapping("/learning/random")
    public ResponseEntity<CardLearningResponseDto> getRandomLearningPage(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam Long deckId,
            @RequestParam DeckMode mode,
            @RequestParam String rankName) {

        CardLearningRequestDto requestDto = new CardLearningRequestDto(
                user.getId(),
                deckId,
                mode,
                rankName
        );

        CardLearningResponseDto response = cardLearningService.getRandomLearningPage(requestDto);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ 1️⃣ Permanent 모드: 특정 카드 학습 페이지 진입
     * 예: /api/cards/learning/permanent/{cardId}?deckId=1
     */
    @GetMapping("/{cardId}/learning/permanent")
    public ResponseEntity<CardLearningPermanentResponseDto> getPermanentLearningByCardId(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long cardId,
            @RequestParam Long deckId) {

        // DTO를 생성하여 Service로 전달
        CardLearningPermanentRequestDto requestDto = new CardLearningPermanentRequestDto(
                user.getId(), deckId, Optional.of(cardId));

        CardLearningPermanentResponseDto response = cardLearningService.preparePermanentLearning(requestDto);

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ 2️⃣ Permanent 모드: 랜덤 학습 페이지 진입
     * 예: /api/cards/learning/permanent/random?deckId=1
     */
    @GetMapping("/learning/permanent/random")
    public ResponseEntity<CardLearningPermanentResponseDto> getPermanentRandomLearning(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam Long deckId) {

        // DTO를 생성하여 Service로 전달
        CardLearningPermanentRequestDto requestDto = new CardLearningPermanentRequestDto(
                user.getId(), deckId);

        CardLearningPermanentResponseDto response = cardLearningService.preparePermanentRandomLearning(requestDto);

        return ResponseEntity.ok(response);
    }
}