package com.example.thirdtool.UserSchedule.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.UserSchedule.domain.exception.UserScheduleDomainException;
import org.springframework.stereotype.Component;

@Component
public class LearningModeMappingPolicy {

    // ─── 매핑 분기점 (도메인 규칙) ────────────────────────────────
    // ≤7   → MODE_7D
    // 8~14 → MODE_14D
    // 15~28→ MODE_28D
    // 29~60→ MODE_60D
    // >60  → silent clamp to 60 → MODE_60D
    private static final int THRESHOLD_7D_MAX  = 7;
    private static final int THRESHOLD_14D_MAX = 14;
    private static final int THRESHOLD_28D_MAX = 28;
    public  static final int CLAMP_MAX_DAYS    = 60;

    public LearningMode resolve(int inputDays) {
        return resolveWithClamp(inputDays).mode();
    }

    public MappingResult resolveWithClamp(int inputDays) {
        validate(inputDays);
        int effectiveDays = Math.min(inputDays, CLAMP_MAX_DAYS);
        boolean clamped = inputDays > CLAMP_MAX_DAYS;
        LearningMode mode = resolveMode(effectiveDays);
        return new MappingResult(mode, effectiveDays, clamped);
    }

    private LearningMode resolveMode(int effectiveDays) {
        if (effectiveDays <= THRESHOLD_7D_MAX)  return LearningMode.MODE_7D;
        if (effectiveDays <= THRESHOLD_14D_MAX) return LearningMode.MODE_14D;
        if (effectiveDays <= THRESHOLD_28D_MAX) return LearningMode.MODE_28D;
        return LearningMode.MODE_60D;
    }

    private void validate(int inputDays) {
        if (inputDays < 1) {
            throw UserScheduleDomainException.of(
                    ErrorCode.USER_SCHEDULE_INPUT_TOO_SHORT,
                    "1 이상의 숫자를 입력해주세요. 입력값=" + inputDays);
        }
    }

    /**
     * mapping 결과 + clamp 발생 여부. Application/DTO 계층에서 사용자 안내에 활용.
     */
    public record MappingResult(LearningMode mode, int effectiveDays, boolean clamped) {}
}
