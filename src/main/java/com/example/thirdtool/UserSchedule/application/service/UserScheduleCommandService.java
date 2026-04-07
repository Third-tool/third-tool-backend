package com.example.thirdtool.UserSchedule.application.service;

import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import com.example.thirdtool.UserSchedule.domain.model.LearningModeMappingPolicy;
import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfig;
import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfigHistoryAppender;
import com.example.thirdtool.UserSchedule.infrastructure.persistence.UserScheduleConfigRepository;
import com.example.thirdtool.UserSchedule.presentation.dto.UserScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserScheduleCommandService {

    private final UserScheduleConfigRepository configRepository;
    private final UserScheduleConfigHistoryAppender historyAppender;
    private final LearningModeMappingPolicy mappingPolicy;

    public UserScheduleResponse.Save save(Long userId, int inputDays) {
        return configRepository.findByUserId(userId)
                               .map(config -> update(config, inputDays))
                               .orElseGet(() -> create(userId, inputDays));
    }

    // ─── 내부 처리 ───────────────────────────────────────────────
    private UserScheduleResponse.Save update(UserScheduleConfig config, int newInputDays) {
        LearningMode before = config.getMappedMode();

        config.updateMode(newInputDays, mappingPolicy);
        configRepository.save(config);

        LearningMode after = config.getMappedMode();
        historyAppender.append(config, before, after, newInputDays);

        return UserScheduleResponse.Save.of(config);
    }

    private UserScheduleResponse.Save create(Long userId, int inputDays) {
        UserScheduleConfig config = UserScheduleConfig.create(userId, inputDays, mappingPolicy);
        configRepository.save(config);

        historyAppender.append(config, null, config.getMappedMode(), inputDays);

        return UserScheduleResponse.Save.of(config);
    }
}
