package com.example.thirdtool.User.infrastructure.kakao;

import com.example.thirdtool.User.domain.model.SocialMember;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("KAKAO")
@Getter
@NoArgsConstructor
public class KakaoMember extends SocialMember {

    @Builder
    public KakaoMember(UserEntity user, String kakaoId, String refreshToken) {
        super(user, kakaoId, refreshToken);
    }

    @Override
    public SocialProviderType getProviderType() {
        return SocialProviderType.KAKAO;
    }
}