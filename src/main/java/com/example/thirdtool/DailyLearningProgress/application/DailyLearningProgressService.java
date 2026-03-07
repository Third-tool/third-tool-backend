package com.example.thirdtool.DailyLearningProgress.application;

import com.example.thirdtool.LegacyCard.Card.domain.model.CardRankType;
import com.example.thirdtool.DailyLearningProgress.domain.DailyLearningProgress;
import com.example.thirdtool.DailyLearningProgress.domain.DailyLearningProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyLearningProgressService {

    private final DailyLearningProgressRepository progressRepository;

    @Transactional
    public DailyLearningProgress getTodayProgress(Long userId) {

        return progressRepository.findByUserId(userId)
                                 .orElseGet(() -> progressRepository.save(DailyLearningProgress.init(userId)));
    }

    @Transactional
    public void increaseRankCount(Long userId, CardRankType rankType) {
        DailyLearningProgress progress = progressRepository.findByUserId(userId)
                                                           .orElseGet(() -> progressRepository.save(DailyLearningProgress.init(userId)));
        progress.increment(rankType);
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    public void resetAllProgressAtMidnight() {
        progressRepository.resetAllProgress();
    }
}