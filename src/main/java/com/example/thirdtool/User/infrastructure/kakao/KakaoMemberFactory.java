package com.example.thirdtool.User.infrastructure.kakao;

import com.example.thirdtool.User.application.SocialMemberFactory;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.SocialUserInfo;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 카카오 SocialMember 저장 어댑터. {@link KakaoMemberRepository}에 영속을 위임.
 */
@Component
@RequiredArgsConstructor
public class KakaoMemberFactory implements SocialMemberFactory {

    private final KakaoMemberRepository kakaoMemberRepository;

    @Override
    public SocialProviderType getProviderType() {
        return SocialProviderType.KAKAO;
    }

    @Override
    public boolean existsBySocialId(String socialId) {
        return kakaoMemberRepository.findBySocialId(socialId).isPresent();
    }

    @Override
    public void register(UserEntity user, SocialUserInfo info) {
        kakaoMemberRepository.save(
                KakaoMember.builder()
                           .user(user)
                           .kakaoId(info.socialId())
                           .build()
        );
    }
}
