package com.example.thirdtool.Card.presentation.controller;

import com.example.thirdtool.Card.application.service.CardService;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.presentation.dto.CardInfoDto;
import com.example.thirdtool.Card.presentation.dto.FeedbackRequestDto;
import com.example.thirdtool.Card.presentation.dto.ResetScoreRequestDto;
import com.example.thirdtool.Card.presentation.dto.WriteCardDto;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards/")
public class CardController {

    private final CardService cardService;
    //카드 단건 조회 api
    @GetMapping("/{cardId}")
    public ResponseEntity<Card> getCardById(@PathVariable("cardId") Long cardId) {
        Card card = cardService.getCardById(cardId);
        return ResponseEntity.ok().body(card);
    }

    // 덱 ID와 학습 모드에 따라 카드 목록을 조회하는 API
    // 덱 모드는 필수입니다!
    // GET /api/cards/decks/{deckId}?mode=THREE_DAY
    // 초기 3day와
    @GetMapping("/decks/{deckId}")
    public ResponseEntity<List<Card>> getCardsByDeckAndMode(
            @PathVariable Long deckId,
            @RequestParam DeckMode mode
                                                           ) {
        List<Card> cards = cardService.getCardsByDeckIdAndMode(deckId, mode);
        return ResponseEntity.ok(cards);
    }

    // ✅ 덱 ID를 기반으로 특정 랭크의 카드 목록을 조회하는 API
    // GET /api/cards/by-rank?deckId=1&rankName=SILVER
    @GetMapping("/by-rank")
    public ResponseEntity<List<CardInfoDto>> getCardsByRank(
            @RequestParam Long deckId, // ✅ deckId 추가
            @RequestParam CardRankType rankName) {

        Long userId = 1L; // 예시
        // deckId를 포함하여 서비스로 전달
        List<CardInfoDto> cards = cardService.getCardsByRank(userId, deckId, rankName);
        return ResponseEntity.ok(cards);
    }

    //카드 가져오기 api
    @GetMapping("/decks/{deckId}/all")
    public ResponseEntity<List<Card>> getCardsById(@PathVariable Long deckId) {
        List<Card> cards=cardService.getCardsByDeckId(deckId);
        return ResponseEntity.ok(cards);
    }

    // 카드 생성 API
    // POST /api/cards/decks/{deckId}
    @PostMapping("/decks/{deckId}")
    public ResponseEntity<Void> createCard(@PathVariable Long deckId,
                                           @RequestBody WriteCardDto writeCardDto) {
        cardService.createCard(deckId, writeCardDto);
        return ResponseEntity.ok().build();
    }
    // ✅ 카드 수정 API
    // PUT /api/cards/{cardId}
    @PutMapping("/{cardId}")
    public ResponseEntity<Void> updateCard(@PathVariable Long cardId,
                                           @RequestBody WriteCardDto writeCardDto) {
        cardService.updateCard(cardId, writeCardDto);
        return ResponseEntity.ok().build();
    }

    // ✅ 카드 삭제 API
    // DELETE /api/cards/{cardId}
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.ok().build();
    }


    // 카드에 대한 학습 피드백 전달 API
    // POST /api/cards/feedback
    @PostMapping("/feedback")
    public ResponseEntity<Void> giveFeedback(@RequestBody FeedbackRequestDto feedbackDto) {
        cardService.giveFeedback(feedbackDto);
        return ResponseEntity.ok().build();
    }


    // ✅ 카드 초기화 및 점수 설정 API
    // POST /api/cards/{cardId}/reset-with-score
    @PostMapping("/{cardId}/reset-with-score")
    public ResponseEntity<Void> resetCardWithScore(@PathVariable Long cardId,
                                                   @RequestBody ResetScoreRequestDto requestDto) {
        cardService.resetCardWithScore(cardId, requestDto.newScore());
        return ResponseEntity.ok().build();
    }

    //카드 랜덤 학습 시스템
    @GetMapping("/decks/{deckId}/learning-session")
    public ResponseEntity<List<Card>> getLearningSession(
            @PathVariable Long deckId,
            @RequestParam DeckMode mode,
            @RequestParam(defaultValue = "10") int count) {

        // Service로부터 List<Card>를 받음
        List<Card> cards = cardService.getTopNLowScoreCardsForLearningSession(deckId, mode, count);

        // JSON 배열 형태로 클라이언트에 반환
        return ResponseEntity.ok(cards);
    }

    // ✅ 랭크, 모드, 개수 기반 학습 세션용 카드 조회 API
    // GET /api/cards/learning-session-by-rank?rankName=SILVER&mode=THREE_DAY&count=10
    @GetMapping("/learning-session-by-rank")
    public ResponseEntity<List<Card>> getLearningSessionByRank(
            @RequestParam String rankName,
            @RequestParam DeckMode mode,
            @RequestParam(defaultValue = "10") int count) {

        // userId는 임시로 하드코딩
        Long userId = 1L;

        List<Card> cards = cardService.getTopNCardsByRankAndModeForLearning(userId, rankName, mode, count);
        return ResponseEntity.ok(cards);
    }
}