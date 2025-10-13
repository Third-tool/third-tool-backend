package com.example.thirdtool.User.infrastructure.Naver;

import com.example.thirdtool.User.domain.model.SocialMember;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@DiscriminatorValue("NAVER")
@Getter
@NoArgsConstructor
public class NaverMember extends SocialMember {

    @Builder
    public NaverMember(UserEntity user, String naverId, String refreshToken) {
        super(user, naverId, refreshToken);
    }

    @Override
    public SocialProviderType getProviderType() {
        return SocialProviderType.NAVER;
    }
}