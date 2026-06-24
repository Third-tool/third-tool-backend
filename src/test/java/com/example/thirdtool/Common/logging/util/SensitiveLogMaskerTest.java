package com.example.thirdtool.Common.logging.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SensitiveLogMasker")
class SensitiveLogMaskerTest {

    @Test
    @DisplayName("null 입력은 'null' 리터럴로 반환한다 (NPE 방지 + 명시 표기)")
    void null_입력은_null_리터럴로_반환한다() {
        assertThat(SensitiveLogMasker.maskToken(null)).isEqualTo("null");
    }

    @Test
    @DisplayName("빈 문자열은 'blank'로 반환한다")
    void 빈_문자열은_blank로_반환한다() {
        assertThat(SensitiveLogMasker.maskToken("")).isEqualTo("blank");
    }

    @Test
    @DisplayName("공백만 있는 문자열은 'blank'로 반환한다")
    void 공백만_있는_문자열은_blank로_반환한다() {
        assertThat(SensitiveLogMasker.maskToken("   \t\n")).isEqualTo("blank");
    }

    @Test
    @DisplayName("8자 이하 토큰은 전부 마스킹된다 (prefix 노출 금지)")
    void 짧은_토큰은_전부_마스킹된다() {
        assertThat(SensitiveLogMasker.maskToken("abcd")).isEqualTo("***");
        assertThat(SensitiveLogMasker.maskToken("12345678")).isEqualTo("***");
    }

    @Test
    @DisplayName("9자 이상 토큰은 prefix 8자 + '***'로 마스킹된다")
    void 긴_토큰은_prefix_8자_뒤를_마스킹한다() {
        String token = "eyJhbGciOiJIUzI1NiJ9.payload.signature";
        assertThat(SensitiveLogMasker.maskToken(token)).isEqualTo("eyJhbGci***");
    }

    @Test
    @DisplayName("마스킹 결과에 원문이 9자 이상에서도 일부만 노출돼 원문 복원 불가능하다")
    void 마스킹_결과는_원문_복원이_불가능하다() {
        String token = "very-long-secret-token-1234567890";
        String masked = SensitiveLogMasker.maskToken(token);
        assertThat(masked).doesNotContain("1234567890");
        assertThat(masked).doesNotContain("secret-token");
    }
}
