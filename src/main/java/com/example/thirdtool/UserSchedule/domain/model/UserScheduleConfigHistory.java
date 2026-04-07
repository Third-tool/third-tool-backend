package com.example.thirdtool.UserSchedule.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.UserSchedule.domain.exception.UserScheduleDomainException;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_schedule_config_history",
        indexes = {
                @Index(
                        name = "idx_user_schedule_config_history_config_id",
                        columnList = "user_schedule_config_id"
                ),
                @Index(
                        name = "idx_user_schedule_config_history_changed_at",
                        columnList = "changed_at"
                )
        }
)
public class UserScheduleConfigHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_schedule_config_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_schedule_config_id", nullable = false, updatable = false)
    private UserScheduleConfig userScheduleConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_mode", updatable = false, length = 20)
    private LearningMode fromMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_mode", nullable = false, updatable = false, length = 20)
    private LearningMode toMode;

    @Column(name = "raw_input_days", nullable = false, updatable = false)
    private int rawInputDays;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    protected UserScheduleConfigHistory() {}

    private UserScheduleConfigHistory(
            UserScheduleConfig userScheduleConfig,
            LearningMode fromMode,
            LearningMode toMode,
            int rawInputDays
                                     ) {
        this.userScheduleConfig = userScheduleConfig;
        this.fromMode           = fromMode;
        this.toMode             = toMode;
        this.rawInputDays       = rawInputDays;
    }

    static UserScheduleConfigHistory of(
            UserScheduleConfig config,
            LearningMode fromMode,
            LearningMode toMode,
            int rawInputDays
                                       ) {
        if (config == null) {
            throw UserScheduleDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "UserScheduleConfigHistory: config는 null일 수 없습니다.");
        }
        if (toMode == null) {
            throw UserScheduleDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "UserScheduleConfigHistory: toMode는 null일 수 없습니다.");
        }
        if (rawInputDays < 1) {
            throw UserScheduleDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "UserScheduleConfigHistory: rawInputDays는 1 이상이어야 합니다. rawInputDays=" + rawInputDays);
        }
        return new UserScheduleConfigHistory(config, fromMode, toMode, rawInputDays);
    }
}
