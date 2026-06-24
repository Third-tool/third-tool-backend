package com.example.thirdtool.User.infrastructure.kakao;

import com.example.thirdtool.Common.logging.util.SensitiveLogMasker;
import com.example.thirdtool.User.infrastructure.kakao.dto.KakaoTokenResponse;
import com.example.thirdtool.User.infrastructure.kakao.dto.KakaoUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class KakaoOAuthClient {

    private final WebClient webClient;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    public KakaoOAuthClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://kauth.kakao.com").build();
    }

    // 1. Authorization Code로 Access Token 획득
    public KakaoTokenResponse getAccessToken(String authorizationCode) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", authorizationCode);

        log.info("[KakaoOAuthClient] 🔍 Token 요청 파라미터: grant_type={}, client_id={}, redirect_uri={}, code={}",
                "authorization_code", clientId, redirectUri, SensitiveLogMasker.maskToken(authorizationCode));

        return webClient.post()
                        .uri("/oauth/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .bodyValue(formData)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, response -> {
                            log.error("[KakaoOAuthClient] ❌ 4xx 오류 발생: {}", response.statusCode());
                            return response.bodyToMono(String.class)
                                           .flatMap(body -> {
                                               // 본문에 access_token이 포함될 수 있으므로 길이만 노출
                                               log.error("[KakaoOAuthClient] ❌ 응답 본문 length={}", body == null ? 0 : body.length());
                                               return Mono.error(new RuntimeException("카카오 토큰 요청 실패"));
                                           });
                        })
                        .bodyToMono(KakaoTokenResponse.class)
                        .block(); // 비동기 호출을 동기식으로 블록킹 (프로덕션에서는 Mono로 처리 권장)
    }

    // 2. Access Token으로 사용자 정보 조회
    public KakaoUserInfo getUserInfo(String accessToken) {
        WebClient userInfoClient = WebClient.builder().baseUrl("https://kapi.kakao.com").build();

        return userInfoClient.get()
                             .uri("/v2/user/me")
                             .header("Authorization", "Bearer " + accessToken)
                             .retrieve()
                             .bodyToMono(KakaoUserInfo.class)
                             .block(); // 비동기 호출을 동기식으로 블록킹
    }
}
