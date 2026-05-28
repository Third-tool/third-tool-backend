package com.example.thirdtool.User.infrastructure.Naver;

import com.example.thirdtool.User.application.SocialMemberFactory;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.SocialUserInfo;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 네이버 SocialMember 저장 어댑터. {@link NaverMemberRepository}에 영속을 위임.
 */
@Component
@RequiredArgsConstructor
public class NaverMemberFactory implements SocialMemberFactory {

    private final NaverMemberRepository naverMemberRepository;

    @Override
    public SocialProviderType getProviderType() {
        return SocialProviderType.NAVER;
    }

    @Override
    public boolean existsBySocialId(String socialId) {
        return naverMemberRepository.findBySocialId(socialId).isPresent();
    }

    @Override
    public void register(UserEntity user, SocialUserInfo info) {
        naverMemberRepository.save(
                NaverMember.builder()
                           .user(user)
                           .naverId(info.socialId())
                           .build()
        );
    }
}
