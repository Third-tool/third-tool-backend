package com.example.thirdtool.User.presentation;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.User.domain.exception.UserDomainException;
import com.example.thirdtool.User.infrastructure.Naver.NaverOAuthClient;
import com.example.thirdtool.User.infrastructure.Naver.dto.NaverTokenResponse;
import com.example.thirdtool.User.infrastructure.Naver.dto.NaverUserInfo;
import com.example.thirdtool.User.infrastructure.kakao.KakaoOAuthClient;
import com.example.thirdtool.User.infrastructure.kakao.dto.KakaoTokenResponse;
import com.example.thirdtool.User.infrastructure.kakao.dto.KakaoUserInfo;
import com.example.thirdtool.User.application.UserService;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.Common.security.auth.dto.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
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
            @RequestBody Map<String, String> payload,
            HttpServletResponse response) {

        String code = payload.get("code");
        String state = payload.getOrDefault("state", null); // 네이버만 사용
        SocialProviderType providerType;
        try {
            providerType = SocialProviderType.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException ex) {
            // 사용자 입력값은 응답 메시지에 echo하지 않고 서버 로그에만 기록 (input reflection 방지).
            log.warn("Unsupported social provider requested: {}", provider);
            throw UserDomainException.of(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }

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
            default -> throw UserDomainException.of(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }

        // ✅ 회원가입 or 로그인 + AT Cookie + RT 바디 발급
        TokenResponse tokens = userService.socialLogin(
                providerType,
                socialId,
                nickname,
                email,
                response
                                                      );

        return ResponseEntity.ok(tokens);
    }
}
