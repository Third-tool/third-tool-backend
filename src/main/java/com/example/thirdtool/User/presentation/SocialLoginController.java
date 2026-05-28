package com.example.thirdtool.User.presentation;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.security.auth.dto.TokenResponse;
import com.example.thirdtool.User.application.SocialOAuthFlow;
import com.example.thirdtool.User.application.UserService;
import com.example.thirdtool.User.domain.exception.UserDomainException;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.SocialUserInfo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 소셜 로그인 통합 엔드포인트.
 *
 * <p>Story-4-2: 제공자별 분기({@code switch(providerType)})를 제거하고
 * {@link SocialOAuthFlow} 구현체를 {@code Map<SocialProviderType, SocialOAuthFlow>}로
 * 주입받아 단일 호출로 처리. 신규 제공자 추가는 {@link SocialOAuthFlow} 구현체
 * 1개를 {@link org.springframework.stereotype.Component @Component} 등록만 하면
 * 자동으로 Map에 합류.
 */
@Slf4j
@RestController
@RequestMapping("/social")
public class SocialLoginController {

    private final UserService userService;
    private final Map<SocialProviderType, SocialOAuthFlow> oauthFlows;

    public SocialLoginController(UserService userService, List<SocialOAuthFlow> flows) {
        this.userService = userService;
        this.oauthFlows = flows.stream()
                               .collect(Collectors.toMap(SocialOAuthFlow::getProviderType, Function.identity()));
    }

    /**
     * 소셜 로그인 통합 엔드포인트.
     * - provider: "kakao" or "naver"
     * - code, state (네이버용)
     */
    @PostMapping(value = "/login/{provider}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenResponse> socialLogin(
            @PathVariable("provider") String provider,
            @RequestBody Map<String, String> payload,
            HttpServletResponse response) {

        String code = payload.get("code");
        String state = payload.getOrDefault("state", null);
        SocialProviderType providerType = resolveProvider(provider);

        SocialOAuthFlow flow = oauthFlows.get(providerType);
        if (flow == null) {
            // 등록된 SocialOAuthFlow 구현체가 없는 경우 — 신규 제공자 enum이
            // 추가됐는데 구현체가 누락된 상황. 운영자에게 빠른 감지를 위해 경고 로그.
            log.warn("No SocialOAuthFlow registered for provider: {}", providerType);
            throw UserDomainException.of(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }

        SocialUserInfo info = flow.authenticate(code, state);
        TokenResponse tokens = userService.socialLogin(
                info.provider(),
                info.socialId(),
                info.nickname(),
                info.email(),
                response
        );
        return ResponseEntity.ok(tokens);
    }

    private SocialProviderType resolveProvider(String provider) {
        try {
            return SocialProviderType.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException ex) {
            // 사용자 입력값은 응답 메시지에 echo하지 않고 서버 로그에만 기록 (input reflection 방지).
            log.warn("Unsupported social provider requested: {}", provider);
            throw UserDomainException.of(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }
    }
}
