package com.example.thirdtool.User.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocialUserInfoTest {

    @Test
    void create_valid_생성성공() {
        SocialUserInfo info = new SocialUserInfo("12345", "Alice", "a@test", SocialProviderType.KAKAO);

        assertThat(info.socialId()).isEqualTo("12345");
        assertThat(info.nickname()).isEqualTo("Alice");
        assertThat(info.email()).isEqualTo("a@test");
        assertThat(info.provider()).isEqualTo(SocialProviderType.KAKAO);
    }

    @Test
    void create_nickname_null_허용() {
        // 제공자별 닉네임 미동의 케이스
        SocialUserInfo info = new SocialUserInfo("12345", null, "a@test", SocialProviderType.NAVER);
        assertThat(info.nickname()).isNull();
    }

    @Test
    void create_email_null_허용() {
        // 제공자별 이메일 미동의 케이스
        SocialUserInfo info = new SocialUserInfo("12345", "Alice", null, SocialProviderType.KAKAO);
        assertThat(info.email()).isNull();
    }

    @Test
    void create_socialId_null_예외() {
        assertThatThrownBy(() -> new SocialUserInfo(null, "Alice", "a@test", SocialProviderType.KAKAO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("socialId");
    }

    @Test
    void create_socialId_blank_예외() {
        assertThatThrownBy(() -> new SocialUserInfo("  ", "Alice", "a@test", SocialProviderType.KAKAO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("socialId");
    }

    @Test
    void create_provider_null_예외() {
        assertThatThrownBy(() -> new SocialUserInfo("12345", "Alice", "a@test", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider");
    }
}
