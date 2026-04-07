package com.example.thirdtool.UserSchedule.domain.model;

import com.example.thirdtool.UserSchedule.infrastructure.persistence.UserScheduleConfigHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserScheduleConfigHistoryAppender {

    private final UserScheduleConfigHistoryRepository historyRepository;

    public void append(
            UserScheduleConfig config,
            LearningMode fromMode,
            LearningMode toMode,
            int rawInputDays
                      ) {
        // 검증은 UserScheduleConfigHistory.of() 내부에서 수행한다.
        UserScheduleConfigHistory history =
                UserScheduleConfigHistory.of(config, fromMode, toMode, rawInputDays);
        historyRepository.save(history);
    }
}