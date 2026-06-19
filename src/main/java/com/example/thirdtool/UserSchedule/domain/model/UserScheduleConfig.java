package com.example.thirdtool.UserSchedule.domain.model;

import com.example.thirdtool.Card.domain.model.OnFieldBudget;
import com.example.thirdtool.Card.domain.model.SoftScheduleTemplate;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.UserSchedule.domain.exception.UserScheduleDomainException;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_schedule_config",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_schedule_config_user_id",
                columnNames = "user_id"
        )
)
public class UserScheduleConfig {

    // ─── 기본값 상수 ──────────────────────────────────────────────
    private static final int DEFAULT_INPUT_DAYS = 10;
    private static final int DEFAULT_DAILY_TARGET = 20;

    // ─── 식별자 ──────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_schedule_config_id")
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    // ─── 설정 값 ─────────────────────────────────────────────────

    @Column(name = "raw_input_days", nullable = false)
    private int rawInputDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapped_mode", nullable = false, length = 20)
    private LearningMode mappedMode;

    // Story 6-2 — 하루 학습 목표 카드 수. 동적 비율 추천의 분모.
    @Column(name = "daily_target", nullable = false)
    private int dailyTarget = DEFAULT_DAILY_TARGET;

    // ─── 타임스탬프 ───────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** JPA 전용 기본 생성자. 외부에서 직접 사용 금지. */
    protected UserScheduleConfig() {}

    private UserScheduleConfig(Long userId, int rawInputDays, LearningMode mappedMode) {
        this.userId       = userId;
        this.rawInputDays = rawInputDays;
        this.mappedMode   = mappedMode;
        this.dailyTarget  = DEFAULT_DAILY_TARGET;
    }

    // ─── 생성 ────────────────────────────────────────────────────

    public static UserScheduleConfig create(
            Long userId,
            int inputDays,
            LearningModeMappingPolicy policy
                                           ) {
        validateUserId(userId);
        // inputDays 검증 및 매핑은 policy.resolve() 내부에서 수행
        LearningMode mode = policy.resolve(inputDays);
        return new UserScheduleConfig(userId, inputDays, mode);
    }

    public static UserScheduleConfig createDefault(Long userId, LearningModeMappingPolicy policy) {
        validateUserId(userId);
        LearningMode mode = policy.resolve(DEFAULT_INPUT_DAYS);
        return new UserScheduleConfig(userId, DEFAULT_INPUT_DAYS, mode);
    }

    // ─── 수정 ────────────────────────────────────────────────────

    public void updateMode(int newInputDays, LearningModeMappingPolicy policy) {
        LearningMode newMode = policy.resolve(newInputDays);
        this.rawInputDays = newInputDays;
        this.mappedMode   = newMode;
    }

    /**
     * Story 6-2 — 하루 학습 목표 카드 수를 갱신한다. 1 이상 강제.
     */
    public void updateDailyTarget(int newDailyTarget) {
        if (newDailyTarget < 1) {
            throw UserScheduleDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "dailyTarget은 1 이상이어야 합니다. 입력값=" + newDailyTarget);
        }
        this.dailyTarget = newDailyTarget;
    }

    // ─── 파생값 제공 ─────────────────────────────────────────────

    public OnFieldBudget resolveOnFieldBudget() {
        return mappedMode.toOnFieldBudget();
    }

    public SoftScheduleTemplate resolveSoftScheduleTemplate() {
        return mappedMode.toSoftScheduleTemplate();
    }

    // ─── 내부 유틸 ───────────────────────────────────────────────

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw UserScheduleDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "UserScheduleConfig: userId는 null일 수 없습니다.");
        }
    }
}
