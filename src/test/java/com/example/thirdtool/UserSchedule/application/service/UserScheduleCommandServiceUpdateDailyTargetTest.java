package com.example.thirdtool.UserSchedule.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.UserSchedule.domain.exception.UserScheduleDomainException;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import com.example.thirdtool.UserSchedule.domain.model.LearningModeMappingPolicy;
import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfig;
import com.example.thirdtool.UserSchedule.domain.model.UserScheduleConfigHistoryAppender;
import com.example.thirdtool.UserSchedule.infrastructure.persistence.UserScheduleConfigRepository;
import com.example.thirdtool.UserSchedule.presentation.dto.UserScheduleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserScheduleCommandService.updateDailyTarget — Story 6-2/6-3 매트릭스.
 */
@DisplayName("UserScheduleCommandService — updateDailyTarget (Story 6-2/6-3)")
class UserScheduleCommandServiceUpdateDailyTargetTest {

    private UserScheduleConfigRepository configRepository;
    private UserScheduleConfigHistoryAppender historyAppender;
    private LearningModeMappingPolicy mappingPolicy;
    private UserScheduleCommandService service;

    @BeforeEach
    void setUp() {
        configRepository = mock(UserScheduleConfigRepository.class);
        historyAppender  = mock(UserScheduleConfigHistoryAppender.class);
        mappingPolicy    = new LearningModeMappingPolicy();
        service = new UserScheduleCommandService(configRepository, historyAppender, mappingPolicy);

        // save가 호출되면 그대로 반환
        when(configRepository.save(any(UserScheduleConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("기존 설정 보유: dailyTarget 갱신 + 저장 + history 미발생(mode 미변경)")
    void updateDailyTarget_existing_updates_noHistory() {
        UserScheduleConfig config = UserScheduleConfig.create(1L, 10, mappingPolicy);
        when(configRepository.findByUserId(eq(1L))).thenReturn(Optional.of(config));

        UserScheduleResponse.Save response = service.updateDailyTarget(1L, 35);

        assertThat(config.getDailyTarget()).isEqualTo(35);
        assertThat(response.schedule().dailyTarget()).isEqualTo(35);
        verify(configRepository, times(1)).save(config);
        // mode 변경이 없으므로 history 미발생
        verify(historyAppender, never()).append(any(), any(), any(), any() == null ? 0 : 0);
    }

    @Test
    @DisplayName("미보유 사용자: createDefault 후 dailyTarget만 갱신 + 초기 생성 history 1건")
    void updateDailyTarget_missing_createsDefault_thenUpdates() {
        when(configRepository.findByUserId(eq(2L))).thenReturn(Optional.empty());

        UserScheduleResponse.Save response = service.updateDailyTarget(2L, 50);

        assertThat(response.schedule().dailyTarget()).isEqualTo(50);
        assertThat(response.schedule().mappedMode()).isEqualTo(LearningMode.MODE_10D.name());
        // createDefault에서 1회, updateDailyTarget 부분에서 추가 save 1회 → 총 2회
        verify(configRepository, times(2)).save(any(UserScheduleConfig.class));
        // 초기 생성 history만 1회 (dailyTarget 변경 자체는 mode history 미생성)
        verify(historyAppender, times(1)).append(any(), eq(null), eq(LearningMode.MODE_10D), eq(10));
    }

    @Test
    @DisplayName("dailyTarget 0/음수는 도메인이 INVALID_INPUT 예외 + repository.save 호출 안 됨")
    void updateDailyTarget_invalid_throwsAndNoSave() {
        UserScheduleConfig config = UserScheduleConfig.create(1L, 10, mappingPolicy);
        when(configRepository.findByUserId(eq(1L))).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> service.updateDailyTarget(1L, 0))
                .isInstanceOf(UserScheduleDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        // 도메인 예외가 save 전에 발생 → repository.save 미호출
        verify(configRepository, never()).save(config);
    }
}
