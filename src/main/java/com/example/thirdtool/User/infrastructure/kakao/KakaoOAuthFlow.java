package com.example.thirdtool.User.infrastructure.kakao;

import com.example.thirdtool.User.application.SocialOAuthFlow;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.SocialUserInfo;
import com.example.thirdtool.User.infrastructure.kakao.dto.KakaoTokenResponse;
import com.example.thirdtool.User.infrastructure.kakao.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 카카오 OAuth 인증 흐름. {@link KakaoOAuthClient}에 외부 API 호출을 위임하고
 * 응답을 표준 {@link SocialUserInfo}로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class KakaoOAuthFlow implements SocialOAuthFlow {

    private final KakaoOAuthClient kakaoOAuthClient;

    @Override
    public SocialProviderType getProviderType() {
        return SocialProviderType.KAKAO;
    }

    @Override
    public SocialUserInfo authenticate(String code, String state) {
        // 카카오는 state를 사용하지 않음.
        KakaoTokenResponse tokenResponse = kakaoOAuthClient.getAccessToken(code);
        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(tokenResponse.getAccess_token());
        return new SocialUserInfo(
                userInfo.getId(),
                userInfo.getNickname(),
                userInfo.getEmail(),
                SocialProviderType.KAKAO
        );
    }
}
