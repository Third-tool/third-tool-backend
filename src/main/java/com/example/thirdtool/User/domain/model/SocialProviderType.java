package com.example.thirdtool.User.domain.model;

import lombok.Getter;

@Getter
public enum SocialProviderType {

    KAKAO("카카오"),
    NAVER("네이버");

    private final String description;

    SocialProviderType(String description) {
        this.description = description;
    }

}