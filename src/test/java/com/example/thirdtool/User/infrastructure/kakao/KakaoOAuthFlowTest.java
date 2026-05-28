package com.example.thirdtool.User.infrastructure.kakao;

import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.SocialUserInfo;
import com.example.thirdtool.User.infrastructure.kakao.dto.KakaoTokenResponse;
import com.example.thirdtool.User.infrastructure.kakao.dto.KakaoUserInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KakaoOAuthFlowTest {

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @InjectMocks
    private KakaoOAuthFlow flow;

    @Test
    void getProviderType_KAKAO_반환() {
        assertThat(flow.getProviderType()).isEqualTo(SocialProviderType.KAKAO);
    }

    @Test
    void authenticate_정상응답_SocialUserInfo로_변환() {
        KakaoTokenResponse token = new KakaoTokenResponse();
        token.setAccess_token("AT123");

        KakaoUserInfo kakaoUserInfo = new KakaoUserInfo();
        kakaoUserInfo.setId("12345");
        // KakaoUserInfo.getNickname/getEmail은 내부 kakao_account/profile 객체 경유.
        // 본 단위 테스트는 변환 매핑만 검증하므로 setter로 직접 채워 넣음.
        KakaoUserInfo.KakaoAccount account = new KakaoUserInfo.KakaoAccount();
        account.setEmail("user@kakao.test");
        KakaoUserInfo.KakaoProfile profile = new KakaoUserInfo.KakaoProfile();
        profile.setNickname("KakaoUser");
        account.setProfile(profile);
        ReflectionTestUtils.setField(kakaoUserInfo, "kakao_account", account);

        given(kakaoOAuthClient.getAccessToken("CODE")).willReturn(token);
        given(kakaoOAuthClient.getUserInfo("AT123")).willReturn(kakaoUserInfo);

        SocialUserInfo result = flow.authenticate("CODE", null);

        assertThat(result.socialId()).isEqualTo("12345");
        assertThat(result.nickname()).isEqualTo("KakaoUser");
        assertThat(result.email()).isEqualTo("user@kakao.test");
        assertThat(result.provider()).isEqualTo(SocialProviderType.KAKAO);
    }

    @Test
    void authenticate_state는_무시되고_getAccessToken_code_단일인자만_호출() {
        // state 인자가 와도 무시되고 KakaoOAuthClient.getAccessToken(code) 단일 인자만 호출됨을 verify로 명시 검증.
        KakaoTokenResponse token = new KakaoTokenResponse();
        token.setAccess_token("AT");
        KakaoUserInfo userInfo = new KakaoUserInfo();
        userInfo.setId("99");

        given(kakaoOAuthClient.getAccessToken("CODE")).willReturn(token);
        given(kakaoOAuthClient.getUserInfo("AT")).willReturn(userInfo);

        SocialUserInfo result = flow.authenticate("CODE", "RANDOM_STATE_IGNORED");

        assertThat(result.socialId()).isEqualTo("99");
        // KakaoOAuthClient.getAccessToken은 정확히 "CODE" 인자로 1회 호출 — state는 어디에도 전달되지 않음.
        verify(kakaoOAuthClient).getAccessToken("CODE");
        verify(kakaoOAuthClient).getUserInfo("AT");
    }
}
