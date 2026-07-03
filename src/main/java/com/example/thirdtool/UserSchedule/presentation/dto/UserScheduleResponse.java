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
            String mappingGuide,
            boolean wasClamped
    ) {
        public static Save of(UserScheduleConfig config) {
            return of(config, false);
        }

        /**
         * Story-CARD-E1-S1-5 — 60일 상한 clamp 여부를 프론트에 전달한다.
         * clamp된 경우 `wasClamped=true`가 되고, mappingGuide 문구에도 clamp 안내가 추가된다.
         */
        public static Save of(UserScheduleConfig config, boolean wasClamped) {
            return new Save(
                    ScheduleDto.of(config),
                    config.getUpdatedAt(),
                    buildMappingGuide(config, wasClamped),
                    wasClamped
            );
        }

        private static String buildMappingGuide(UserScheduleConfig config, boolean wasClamped) {
            int inputDays   = config.getRawInputDays();
            int modeDays    = config.getMappedMode().maxDays();
            String modeName = config.getMappedMode().getDisplayName();

            if (wasClamped) {
                return inputDays + "일을 입력하셨습니다. 최대 60일까지 지원되어 " + modeName + "로 운영됩니다.";
            }
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
            int dailyTarget,
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
                    mode.stepCount(),
                    mode.maxDays(),
                    config.getDailyTarget(),
                    intervals
            );
        }
    }
}