package com.example.thirdtool.Common.logging.util;

/**
 * 로그 출력 시 민감정보를 마스킹하는 유틸 (Story 3-1).
 *
 * <p>비밀번호·access token·refresh token·OAuth 인가 코드 등 원문이 로그에 그대로
 * 노출되면 prod JSON 스트림 유출 시 즉시 사고로 이어진다. 본 유틸은 디버깅 식별성을
 * 유지하면서 원문을 차단하기 위해 <b>prefix 8자 + {@value #MASK_SUFFIX}</b> 패턴을 사용한다.
 *
 * <p>자동 마스킹(Loggable 어노테이션·리플렉션 기반 PII 필터)은 v2 도입 예정. 현 단계는
 * 호출자가 명시적으로 본 유틸을 거치는 컨벤션.
 */
public final class SensitiveLogMasker {

    static final String MASK_SUFFIX = "***";
    static final int PREFIX_LEN = 8;
    static final String NULL_REPRESENTATION = "null";
    static final String BLANK_REPRESENTATION = "blank";

    private SensitiveLogMasker() {
    }

    /**
     * 토큰·시크릿류 문자열을 prefix 8자 + "***"로 마스킹한다.
     * 원문 길이가 8자 이하이면 전부 마스킹("***").
     */
    public static String maskToken(String value) {
        if (value == null) {
            return NULL_REPRESENTATION;
        }
        if (value.isBlank()) {
            return BLANK_REPRESENTATION;
        }
        if (value.length() <= PREFIX_LEN) {
            return MASK_SUFFIX;
        }
        return value.substring(0, PREFIX_LEN) + MASK_SUFFIX;
    }
}
