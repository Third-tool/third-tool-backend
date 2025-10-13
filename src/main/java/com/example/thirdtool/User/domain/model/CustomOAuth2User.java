package com.example.thirdtool.User.domain.model;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private final UserEntity user;
    private final Map<String, Object> attributes; // 카카오/네이버 원본 데이터

    public CustomOAuth2User(UserEntity user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> "ROLE_" + user.getRoleType().name());
    }

    @Override
    public String getName() {
        return user.getUsername();
    }

    // 추가 접근자 (편의용)
    public String getUsername() { return user.getUsername(); }
    public String getEmail() { return user.getEmail(); }
    public String getNickname() { return user.getNickname(); }
    public boolean isSocial() { return user.getIsSocial(); }
}
