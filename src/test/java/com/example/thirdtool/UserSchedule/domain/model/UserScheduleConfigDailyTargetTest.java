package com.example.thirdtool.UserSchedule.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.UserSchedule.domain.exception.UserScheduleDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserScheduleConfig — dailyTarget (Story 6-2)")
class UserScheduleConfigDailyTargetTest {

    private final LearningModeMappingPolicy policy = new LearningModeMappingPolicy();

    @Test
    @DisplayName("create — 기본값 dailyTarget=20")
    void create_default_dailyTarget_20() {
        UserScheduleConfig config = UserScheduleConfig.create(1L, 10, policy);

        assertThat(config.getDailyTarget()).isEqualTo(20);
    }

    @Test
    @DisplayName("createDefault — 기본값 dailyTarget=20")
    void createDefault_default_dailyTarget_20() {
        UserScheduleConfig config = UserScheduleConfig.createDefault(1L, policy);

        assertThat(config.getDailyTarget()).isEqualTo(20);
    }

    @Test
    @DisplayName("updateDailyTarget — 1 이상 정상 갱신")
    void updateDailyTarget_valid_updates() {
        UserScheduleConfig config = UserScheduleConfig.create(1L, 10, policy);

        config.updateDailyTarget(50);

        assertThat(config.getDailyTarget()).isEqualTo(50);
    }

    @Test
    @DisplayName("updateDailyTarget — 1 미만은 INVALID_INPUT 예외")
    void updateDailyTarget_zero_throws() {
        UserScheduleConfig config = UserScheduleConfig.create(1L, 10, policy);

        assertThatThrownBy(() -> config.updateDailyTarget(0))
                .isInstanceOf(UserScheduleDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("updateDailyTarget — 음수도 INVALID_INPUT")
    void updateDailyTarget_negative_throws() {
        UserScheduleConfig config = UserScheduleConfig.create(1L, 10, policy);

        assertThatThrownBy(() -> config.updateDailyTarget(-5))
                .isInstanceOf(UserScheduleDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
