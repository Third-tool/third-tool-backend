package com.example.thirdtool.User.infrastructure.Naver;

import com.example.thirdtool.User.infrastructure.Naver.dto.NaverTokenResponse;
import com.example.thirdtool.User.infrastructure.Naver.dto.NaverUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class NaverOAuthClient {

    private final WebClient webClient;

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    @Value("${naver.redirect-uri}")
    private String redirectUri;

    public NaverOAuthClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://nid.naver.com").build();
    }

    // 1️⃣ Authorization Code로 Access Token 요청
    public NaverTokenResponse getAccessToken(String authorizationCode, String state) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", authorizationCode);
        formData.add("state", state);

        return webClient.post()
                        .uri("/oauth2.0/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .bodyValue(formData)
                        .retrieve()
                        .bodyToMono(NaverTokenResponse.class)
                        .block();
    }

    // 2️⃣ Access Token으로 유저 정보 조회
    public NaverUserInfo getUserInfo(String accessToken) {
        WebClient userInfoClient = WebClient.builder()
                                            .baseUrl("https://openapi.naver.com")
                                            .build();

        return userInfoClient.get()
                             .uri("/v1/nid/me")
                             .header("Authorization", "Bearer " + accessToken)
                             .retrieve()
                             .bodyToMono(NaverUserInfo.class)
                             .block();
    }
}