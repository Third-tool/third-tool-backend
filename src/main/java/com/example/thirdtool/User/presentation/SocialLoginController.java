package com.example.thirdtool.User.presentation;

import com.example.thirdtool.User.infrastructure.Naver.NaverOAuthClient;
import com.example.thirdtool.User.infrastructure.Naver.dto.NaverTokenResponse;
import com.example.thirdtool.User.infrastructure.Naver.dto.NaverUserInfo;
import com.example.thirdtool.User.infrastructure.kakao.KakaoOAuthClient;
import com.example.thirdtool.User.infrastructure.kakao.dto.KakaoTokenResponse;
import com.example.thirdtool.User.infrastructure.kakao.dto.KakaoUserInfo;
import com.example.thirdtool.User.application.UserService;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class SocialLoginController {

    private final UserService userService;
    private final KakaoOAuthClient kakaoOauthClient;
    private final NaverOAuthClient naverOauthClient;

    /**
     * 소셜 로그인 통합 엔드포인트
     * - provider: "kakao" or "naver"
     * - code, state (네이버용)
     */
    @PostMapping(value = "/login/{provider}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> socialLogin(
            @PathVariable("provider") String provider,
            @RequestBody Map<String, String> payload) {

        String code = payload.get("code");
        String state = payload.getOrDefault("state", null); // 네이버만 사용
        SocialProviderType providerType = SocialProviderType.valueOf(provider.toUpperCase());

        String accessToken;
        String socialId;
        String nickname;
        String email;

        switch (providerType) {
            case KAKAO -> {
                KakaoTokenResponse tokenResponse = kakaoOauthClient.getAccessToken(code);
                accessToken = tokenResponse.getAccess_token();
                KakaoUserInfo userInfo = kakaoOauthClient.getUserInfo(accessToken);

                socialId = userInfo.getId();
                nickname = userInfo.getNickname();
                email = userInfo.getEmail();
            }
            case NAVER -> {
                NaverTokenResponse tokenResponse = naverOauthClient.getAccessToken(code, state);
                accessToken = tokenResponse.getAccess_token();
                NaverUserInfo userInfo = naverOauthClient.getUserInfo(accessToken);

                socialId = userInfo.getId();
                nickname = userInfo.getNickname();
                email = userInfo.getEmail();
            }
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + provider);
        }

        // ✅ 회원가입 or 로그인 + JWT 발급
        TokenResponse tokens = userService.socialLogin(
                providerType,
                socialId,
                nickname,
                email
                                                      );

        return ResponseEntity.ok(tokens);
    }
}
