package com.example.thirdtool.DailyLearningProgress.presentation;

import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.DailyLearningProgress.application.DailyLearningProgressService;
import com.example.thirdtool.DailyLearningProgress.domain.DailyLearningProgress;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning/progress")
public class DailyLearningProgressController {

    private final DailyLearningProgressService progressService;

    /**
     * ✅ 오늘 학습 현황 조회
     * 예: /api/learning/progress/today
     */
    @GetMapping("/today")
    public ResponseEntity<DailyLearningProgress> getTodayProgress(
            @AuthenticationPrincipal UserEntity user) {

        return ResponseEntity.ok(progressService.getTodayProgress(user.getId()));
    }

    /**
     * ✅ 랭크별 학습 카운트 증가
     * 예: /api/learning/progress/increase?rank=SILVER
     */
    @PostMapping("/increase")
    public ResponseEntity<Void> increaseRankCount(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam CardRankType rank) {

        progressService.increaseRankCount(user.getId(), rank);
        return ResponseEntity.ok().build();
    }
}