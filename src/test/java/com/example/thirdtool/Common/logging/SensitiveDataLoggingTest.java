package com.example.thirdtool.Common.logging;

import com.example.thirdtool.Common.security.auth.RefreshEntity;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.dto.LoginRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 3-1: 민감정보(비밀번호·refresh 토큰)가 toString 직렬화 경로 어디서도 노출되지
 * 않도록 회귀 방지. 신규 민감 DTO/엔티티 추가 시 본 테스트에 케이스 추가가 PR 체크리스트.
 *
 * <p>{@code @ToString.Exclude} 자체는 Lombok SOURCE retention이라 리플렉션으로 검증할 수
 * 없으므로 functional 검증(실제 toString 결과에 원문이 포함되지 않는다)으로 회귀를 잡는다.
 */
@DisplayName("민감정보 로깅 차단")
class SensitiveDataLoggingTest {

    private static final String RAW_PASSWORD = "mySecret123!verySensitive";
    private static final String RAW_HASHED = "$2a$10$BCryptHashedPasswordHere1234567890";
    private static final String RAW_REFRESH = "rt.eyJhbGciOiJIUzI1NiJ9.payload.signature-FULL-SECRET";

    @Test
    @DisplayName("LoginRequestDTO.toString은 password 원문을 절대 포함하지 않는다 (명시 오버라이드)")
    void LoginRequestDTO_toString은_password_원문을_절대_포함하지_않는다() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("user@example.com");
        dto.setPassword(RAW_PASSWORD);

        String dumped = dto.toString();

        assertThat(dumped).doesNotContain(RAW_PASSWORD);
        assertThat(dumped).contains("password=***");
        assertThat(dumped).contains("username=user@example.com");
    }

    @Test
    @DisplayName("UserEntity.toString은 password 해시를 노출하지 않는다 (@ToString 미사용 + @ToString.Exclude 안전망)")
    void UserEntity_toString은_password_해시를_노출하지_않는다() {
        UserEntity user = UserEntity.ofLocal("user@example.com", RAW_HASHED, "nickname", "user@example.com");

        String dumped = user.toString();

        assertThat(dumped)
                .as("password BCrypt 해시가 toString 결과 어디에도 등장하지 않아야 한다")
                .doesNotContain(RAW_HASHED);
    }

    @Test
    @DisplayName("RefreshEntity.toString은 refresh 토큰 원문을 노출하지 않는다")
    void RefreshEntity_toString은_refresh_토큰을_노출하지_않는다() {
        RefreshEntity entity = RefreshEntity.ofNew("user@example.com", RAW_REFRESH);

        String dumped = entity.toString();

        assertThat(dumped)
                .as("RT 원문 문자열이 toString 결과 어디에도 등장하지 않아야 한다")
                .doesNotContain(RAW_REFRESH);
    }
}
