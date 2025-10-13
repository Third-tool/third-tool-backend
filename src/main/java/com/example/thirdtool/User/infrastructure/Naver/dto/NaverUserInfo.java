package com.example.thirdtool.User.infrastructure.Naver.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NaverUserInfo {

    private NaverResponse response;

    @Getter
    @Setter
    public static class NaverResponse {
        private String id;
        private String email;
        private String nickname;
        private String profile_image;
    }

    // ✅ 편의 메서드 (KakaoUserInfo와 동일한 스타일)
    public String getId() {
        return response != null ? response.getId() : null;
    }

    public String getEmail() {
        return response != null ? response.getEmail() : null;
    }

    public String getNickname() {
        return response != null ? response.getNickname() : null;
    }

    public String getProfileImage() {
        return response != null ? response.getProfile_image() : null;
    }
}
