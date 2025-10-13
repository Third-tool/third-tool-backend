package com.example.thirdtool.User.infrastructure.kakao.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class KakaoUserInfo {
    private String id;
    private KakaoAccount kakao_account;

    @Getter
    @Setter
    public static class KakaoAccount {
        private String email;
        private KakaoProfile profile;
    }

    @Getter
    @Setter
    public static class KakaoProfile {
        private String nickname;
        private String profile_image_url;
    }

    // 편의 메서드
    public String getNickname() {
        return kakao_account != null && kakao_account.getProfile() != null
                ? kakao_account.getProfile().getNickname()
                : null;
    }

    public String getEmail() {
        return kakao_account != null ? kakao_account.getEmail() : null;
    }
}
