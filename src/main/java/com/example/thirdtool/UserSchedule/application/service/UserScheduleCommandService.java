package com.example.thirdtool.UserSchedule.application.service;

import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import com.example.thirdtool.UserSchedule.domain.model.LearningModeMappingPolicy;
import com.example.thirdtool.UserSchedule.domain.model.LearningModeMappingPolicy.MappingResult;
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

    /**
     * 사용자 학습 스케줄 저장/갱신.
     *
     * <p>Story-CARD-E1-S1-5 — inputDays가 60일을 초과하면 policy가 silent clamp를 수행한다.
     * 응답 {@link UserScheduleResponse.Save#wasClamped()}로 프론트에 clamp 여부를 전달한다.
     */
    public UserScheduleResponse.Save save(Long userId, int inputDays) {
        MappingResult mapping = mappingPolicy.resolveWithClamp(inputDays);
        return configRepository.findByUserId(userId)
                               .map(config -> update(config, inputDays, mapping))
                               .orElseGet(() -> create(userId, inputDays, mapping));
    }

    /**
     * Story 6-3 후속 — 사용자 dailyTarget 갱신. 미보유 유저는 기본 모드(MODE_14D)로 자동 생성한 뒤
     * dailyTarget만 갱신한다. dailyTarget 변경 자체는 v1에서 mode 이력에 기록하지 않는다
     * (mode/inputDays 변경 이력만 추적 — Spec Story 4-2 §3).
     */
    public UserScheduleResponse.Save updateDailyTarget(Long userId, int newDailyTarget) {
        UserScheduleConfig config = configRepository.findByUserId(userId)
                                                    .orElseGet(() -> createDefault(userId));
        config.updateDailyTarget(newDailyTarget);
        configRepository.save(config);
        return UserScheduleResponse.Save.of(config);
    }

    // ─── 내부 처리 ───────────────────────────────────────────────

    private UserScheduleConfig createDefault(Long userId) {
        UserScheduleConfig config = UserScheduleConfig.createDefault(userId, mappingPolicy);
        configRepository.save(config);
        historyAppender.append(config, null, config.getMappedMode(), config.getRawInputDays());
        return config;
    }

    private UserScheduleResponse.Save update(UserScheduleConfig config, int newInputDays, MappingResult mapping) {
        LearningMode before = config.getMappedMode();

        config.updateMode(newInputDays, mappingPolicy);
        configRepository.save(config);

        LearningMode after = config.getMappedMode();
        historyAppender.append(config, before, after, newInputDays);

        return UserScheduleResponse.Save.of(config, mapping.clamped());
    }

    private UserScheduleResponse.Save create(Long userId, int inputDays, MappingResult mapping) {
        UserScheduleConfig config = UserScheduleConfig.create(userId, inputDays, mappingPolicy);
        configRepository.save(config);

        historyAppender.append(config, null, config.getMappedMode(), inputDays);

        return UserScheduleResponse.Save.of(config, mapping.clamped());
    }
}
