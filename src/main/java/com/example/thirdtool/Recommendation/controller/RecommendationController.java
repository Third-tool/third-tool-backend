package com.example.thirdtool.Recommendation.controller;

import com.example.thirdtool.Recommendation.application.RecommendationService;
import com.example.thirdtool.Recommendation.domain.DeckRecommendation;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /** ✅ 추천 덱 목록 */
    @GetMapping("/decks")
    public ResponseEntity<List<DeckRecommendation>> getRecommendations(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(defaultValue = "3") int limit) {

        return ResponseEntity.ok(recommendationService.recommendDecks(user, limit));
    }

    /** ✅ 특정 덱 추천 이유 상세 보기 */
    @GetMapping("/decks/{deckId}/explain")
    public ResponseEntity<DeckRecommendation> explainRecommendation(
            @PathVariable Long deckId,
            @AuthenticationPrincipal UserEntity user) {

        return ResponseEntity.ok(recommendationService.explainRecommendation(deckId, user));
    }
}