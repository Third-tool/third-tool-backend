package com.example.thirdtool.Card.presentation.controller;

import com.example.thirdtool.Card.application.service.CardFeedbackService;
import com.example.thirdtool.Card.application.service.CardService;
import com.example.thirdtool.Card.presentation.dto.request.FeedbackRequestDto;
import com.example.thirdtool.Card.presentation.dto.request.ResetScoreRequestDto;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards")
public class CardFeedbackController {

    private final CardService cardService;
    private final CardFeedbackService cardFeedbackService;

    @PostMapping("/feedback")
    public ResponseEntity<Void> giveFeedback(@AuthenticationPrincipal UserEntity user
                                                 , @RequestBody FeedbackRequestDto dto) {
        var userId=user.getId();
        cardFeedbackService.giveFeedback(userId, dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/reset-with-score")
    public ResponseEntity<Void> resetCardWithScore(@PathVariable Long cardId,
                                                   @RequestBody ResetScoreRequestDto dto) {
        cardFeedbackService.resetCardWithScore(cardId, dto.newScore());
        return ResponseEntity.ok().build();
    }

}
