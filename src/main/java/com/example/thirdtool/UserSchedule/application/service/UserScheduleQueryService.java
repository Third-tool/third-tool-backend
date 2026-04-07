package com.example.thirdtool.UserSchedule.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.UserSchedule.domain.exception.UserScheduleDomainException;
import com.example.thirdtool.UserSchedule.domain.model.LearningModeMappingPolicy;
import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfig;
import com.example.thirdtool.UserSchedule.infrastructure.persistence.UserScheduleConfigHistoryRepository;
import com.example.thirdtool.UserSchedule.infrastructure.persistence.UserScheduleConfigRepository;
import com.example.thirdtool.UserSchedule.presentation.dto.UserScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserScheduleQueryService {

    // ─── 이력 조회 제한 ─────────────────────────────────────────
    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final int MAX_HISTORY_LIMIT      = 50;

    private final UserScheduleConfigRepository        configRepository;
    private final UserScheduleConfigHistoryRepository historyRepository;
    private final LearningModeMappingPolicy           mappingPolicy;

    public UserScheduleResponse.Get getSchedule(Long userId) {
        UserScheduleConfig config = configRepository.findByUserId(userId)
                                                    .orElseGet(() -> initDefault(userId));
        return UserScheduleResponse.Get.of(config);
    }

    @Transactional(readOnly = true)
    public List<UserScheduleResponse.HistoryItem> getHistory(Long userId, Integer limit) {
        int resolvedLimit = resolveLimit(limit);

        UserScheduleConfig config = configRepository.findByUserId(userId)
                                                    .orElseThrow(() -> UserScheduleDomainException.of(
                                                            ErrorCode.SCHEDULE_NOT_FOUND));

        return historyRepository
                .findByUserScheduleConfig_IdOrderByChangedAtDesc(config.getId(), resolvedLimit)
                .stream()
                .map(UserScheduleResponse.HistoryItem::of)
                .toList();
    }

    // ─── 내부 유틸 ───────────────────────────────────────────────

    private UserScheduleConfig initDefault(Long userId) {
        UserScheduleConfig config = UserScheduleConfig.createDefault(userId, mappingPolicy);
        configRepository.save(config);
        return config;
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_HISTORY_LIMIT;
        }
        if (limit > MAX_HISTORY_LIMIT) {
            throw UserScheduleDomainException.of(
                    ErrorCode.SCHEDULE_HISTORY_LIMIT_EXCEEDED,
                    "한 번에 최대 " + MAX_HISTORY_LIMIT + "건까지 조회할 수 있습니다.");
        }
        return limit;
    }
}