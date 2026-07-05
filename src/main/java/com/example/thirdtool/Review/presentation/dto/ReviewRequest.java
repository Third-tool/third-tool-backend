package com.example.thirdtool.Review.presentation.dto;

/**
 * ReviewRequest — REV E2 · Story 2-4 재편.
 *
 * <p>새 세션 시작은 body 없이 인증만으로 동작 · 배치는 서버가 자동 결정.
 * 향후 배치 명시 지정 요청 필요 시 record 추가.
 */
public class ReviewRequest {
    // 신규 엔드포인트는 모두 path variable + 인증만 사용 · body 없음.
}
