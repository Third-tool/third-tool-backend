package com.example.thirdtool.UserSchedule.presentation.dto;

import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfig;
import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfigHistory;

import java.time.LocalDateTime;
import java.util.List;

public class UserScheduleResponse {

    // ─── 1. 현재 설정 조회 응답 ─────────────────────────────────
    // GET /users/me/schedule → 200 OK
    public record Get(
            ScheduleDto schedule,
            LocalDateTime updatedAt
    ) {
        public static Get of(UserScheduleConfig config) {
            return new Get(
                    ScheduleDto.of(config),
                    config.getUpdatedAt()
            );
        }
    }

    // ─── 2. 설정 저장 응답 ───────────────────────────────────────
    // PUT /users/me/schedule → 200 OK
    public record Save(
            ScheduleDto schedule,
            LocalDateTime updatedAt,
            String mappingGuide
    ) {
        public static Save of(UserScheduleConfig config) {
            return new Save(
                    ScheduleDto.of(config),
                    config.getUpdatedAt(),
                    buildMappingGuide(config)
            );
        }

        private static String buildMappingGuide(UserScheduleConfig config) {
            int inputDays   = config.getRawInputDays();
            int modeDays    = config.getMappedMode().getDurationDays();
            String modeName = config.getMappedMode().getDisplayName();

            if (inputDays == modeDays) {
                return modeName + "로 운영됩니다.";
            }
            return inputDays + "일을 입력하셨습니다. " + modeName + "로 운영됩니다.";
        }
    }

    // ─── 3. 설정 변경 이력 조회 응답 ────────────────────────────
    // GET /users/me/schedule/history → 200 OK
    public record HistoryItem(
            Long historyId,
            String fromMode,
            String toMode,
            int rawInputDays,
            LocalDateTime changedAt
    ) {
        public static HistoryItem of(UserScheduleConfigHistory history) {
            return new HistoryItem(
                    history.getId(),
                    history.getFromMode() == null ? null : history.getFromMode().name(),
                    history.getToMode().name(),
                    history.getRawInputDays(),
                    history.getChangedAt()
            );
        }
    }

    // ─── 공통 중첩 DTO ───────────────────────────────────────────

    public record ScheduleDto(
            int rawInputDays,
            String mappedMode,
            String modeDisplayName,
            int maxView,
            int maxDuration,
            List<Integer> softScheduleIntervals
    ) {
        public static ScheduleDto of(UserScheduleConfig config) {
            LearningMode mode = config.getMappedMode();

            List<Integer> intervals = mode.toSoftScheduleTemplate()
                                          .getIntervalSteps()
                                          .stream()
                                          .map(step -> (int) step.minDuration().toDays())
                                          .toList();

            return new ScheduleDto(
                    config.getRawInputDays(),
                    mode.name(),
                    mode.getDisplayName(),
                    mode.getMaxView(),
                    mode.getDurationDays(),
                    intervals
            );
        }
    }
}