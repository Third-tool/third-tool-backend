package com.example.thirdtool.Card.presentation.controller;

import com.example.thirdtool.Card.application.service.CardFeedbackService;
import com.example.thirdtool.Card.application.service.CardService;
import com.example.thirdtool.Card.presentation.dto.request.FeedbackRequestDto;
import com.example.thirdtool.Card.presentation.dto.request.ResetScoreRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards")
public class CardFeedbackController {

    private final CardService cardService;
    private final CardFeedbackService cardFeedbackService;

    @PostMapping("/feedback")
    public ResponseEntity<Void> giveFeedback(@RequestBody FeedbackRequestDto dto) {
        cardFeedbackService.giveFeedback(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/reset-with-score")
    public ResponseEntity<Void> resetCardWithScore(@PathVariable Long cardId,
                                                   @RequestBody ResetScoreRequestDto dto) {
        cardFeedbackService.resetCardWithScore(cardId, dto.newScore());
        return ResponseEntity.ok().build();
    }

}
