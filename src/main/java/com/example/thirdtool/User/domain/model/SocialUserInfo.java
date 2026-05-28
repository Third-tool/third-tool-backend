package com.example.thirdtool.User.domain.model;

/**
 * 소셜 제공자(카카오·네이버 등)로부터 받은 사용자 식별·프로필 정보를
 * 제공자 의존 없는 표준 VO로 표현한다.
 *
 * <p>Story-4-2에서 도입. {@link SocialOAuthFlow} 구현체가 제공자별
 * 응답(예: {@code KakaoUserInfo}, {@code NaverUserInfo})을 본 VO로 변환해
 * 반환한다. 호출자({@code SocialLoginController})는 본 VO만 알면 된다.
 *
 * <p>불변식:
 * <ul>
 *   <li>{@code socialId}, {@code provider}는 null/blank 금지.
 *   <li>{@code nickname}, {@code email}은 nullable — 제공자별 미제공 가능.
 * </ul>
 *
 * @param socialId 소셜 제공자 측의 고유 사용자 ID
 * @param nickname 표시 닉네임 (nullable)
 * @param email    이메일 (nullable, 제공자별 동의 항목에 따라 누락 가능)
 * @param provider 제공자 종류
 */
public record SocialUserInfo(
        String socialId,
        String nickname,
        String email,
        SocialProviderType provider
) {
    public SocialUserInfo {
        if (socialId == null || socialId.isBlank()) {
            throw new IllegalArgumentException("socialId must not be blank");
        }
        if (provider == null) {
            throw new IllegalArgumentException("provider must not be null");
        }
    }
}
