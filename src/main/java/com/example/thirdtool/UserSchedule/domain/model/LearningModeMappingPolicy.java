package com.example.thirdtool.UserSchedule.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.UserSchedule.domain.exception.UserScheduleDomainException;
import org.springframework.stereotype.Component;

@Component
public class LearningModeMappingPolicy {

    // ─── 매핑 분기점 (도메인 규칙) ────────────────────────────────
    private static final int THRESHOLD_10D_MAX = 14; // 1 ~ 14일 → MODE_10D
    private static final int THRESHOLD_20D_MAX = 24; // 15 ~ 24일 → MODE_20D
    // 25일 이상 → MODE_30D

    public LearningMode resolve(int inputDays) {
        validate(inputDays);

        if (inputDays <= THRESHOLD_10D_MAX) {
            return LearningMode.MODE_10D;
        }
        if (inputDays <= THRESHOLD_20D_MAX) {
            return LearningMode.MODE_20D;
        }
        return LearningMode.MODE_30D;
    }

    // ─── 검증 ─────────────────────────────────────────────────────

    private void validate(int inputDays) {
        if (inputDays < 1) {
            throw UserScheduleDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "1 이상의 숫자를 입력해주세요. 입력값=" + inputDays);
        }
    }
}