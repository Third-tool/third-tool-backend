package com.example.thirdtool.Common.security.auth.token;

/**
 * JWT 검증 결과 분류 (Story 3-2).
 *
 * 기존 boolean 결과로는 만료/위조/타입 불일치를 구분할 수 없어 ErrorCode 세분화 불가.
 */
public enum TokenValidationResult {

    VALID,
    EXPIRED,        // exp claim이 현재 시각보다 과거
    INVALID,        // 서명 불일치 / 파싱 실패 / 잘못된 포맷
    TYPE_MISMATCH;  // ACCESS를 기대했으나 REFRESH (또는 그 반대)

    public boolean isValid() {
        return this == VALID;
    }
}
