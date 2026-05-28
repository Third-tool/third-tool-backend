package com.example.thirdtool.User.infrastructure.Naver;

import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.SocialUserInfo;
import com.example.thirdtool.User.infrastructure.Naver.dto.NaverTokenResponse;
import com.example.thirdtool.User.infrastructure.Naver.dto.NaverUserInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NaverOAuthFlowTest {

    @Mock
    private NaverOAuthClient naverOAuthClient;

    @InjectMocks
    private NaverOAuthFlow flow;

    @Test
    void getProviderType_NAVER_반환() {
        assertThat(flow.getProviderType()).isEqualTo(SocialProviderType.NAVER);
    }

    @Test
    void authenticate_정상응답_SocialUserInfo로_변환() {
        NaverTokenResponse token = new NaverTokenResponse();
        token.setAccess_token("AT_NAVER");

        NaverUserInfo userInfo = new NaverUserInfo();
        NaverUserInfo.NaverResponse response = new NaverUserInfo.NaverResponse();
        response.setId("naver-999");
        response.setNickname("NaverUser");
        response.setEmail("user@naver.test");
        userInfo.setResponse(response);

        given(naverOAuthClient.getAccessToken("CODE", "STATE")).willReturn(token);
        given(naverOAuthClient.getUserInfo("AT_NAVER")).willReturn(userInfo);

        SocialUserInfo result = flow.authenticate("CODE", "STATE");

        assertThat(result.socialId()).isEqualTo("naver-999");
        assertThat(result.nickname()).isEqualTo("NaverUser");
        assertThat(result.email()).isEqualTo("user@naver.test");
        assertThat(result.provider()).isEqualTo(SocialProviderType.NAVER);
    }

    @Test
    void authenticate_state가_NaverOAuthClient에_그대로_전달됨() {
        NaverTokenResponse token = new NaverTokenResponse();
        token.setAccess_token("AT");
        NaverUserInfo userInfo = new NaverUserInfo();
        NaverUserInfo.NaverResponse response = new NaverUserInfo.NaverResponse();
        response.setId("naver-1");
        userInfo.setResponse(response);

        given(naverOAuthClient.getAccessToken("CODE", "STATE_X")).willReturn(token);
        given(naverOAuthClient.getUserInfo("AT")).willReturn(userInfo);

        SocialUserInfo result = flow.authenticate("CODE", "STATE_X");

        assertThat(result.socialId()).isEqualTo("naver-1");
        // Mockito stubbing이 정확한 인자(CODE, STATE_X)로 매치돼야 호출 성공
    }
}
