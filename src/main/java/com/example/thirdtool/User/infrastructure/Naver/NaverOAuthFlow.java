package com.example.thirdtool.User.infrastructure.Naver;

import com.example.thirdtool.User.application.SocialOAuthFlow;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.SocialUserInfo;
import com.example.thirdtool.User.infrastructure.Naver.dto.NaverTokenResponse;
import com.example.thirdtool.User.infrastructure.Naver.dto.NaverUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 네이버 OAuth 인증 흐름. {@link NaverOAuthClient}에 외부 API 호출을 위임하고
 * 응답을 표준 {@link SocialUserInfo}로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class NaverOAuthFlow implements SocialOAuthFlow {

    private final NaverOAuthClient naverOAuthClient;

    @Override
    public SocialProviderType getProviderType() {
        return SocialProviderType.NAVER;
    }

    @Override
    public SocialUserInfo authenticate(String code, String state) {
        NaverTokenResponse tokenResponse = naverOAuthClient.getAccessToken(code, state);
        NaverUserInfo userInfo = naverOAuthClient.getUserInfo(tokenResponse.getAccess_token());
        return new SocialUserInfo(
                userInfo.getId(),
                userInfo.getNickname(),
                userInfo.getEmail(),
                SocialProviderType.NAVER
        );
    }
}
