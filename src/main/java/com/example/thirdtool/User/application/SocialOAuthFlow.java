package com.example.thirdtool.User.application;

import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.SocialUserInfo;

/**
 * 소셜 OAuth 인증 흐름의 application 포트.
 *
 * <p>Story-4-2: SocialLoginController가 제공자별 분기(switch) 없이
 * {@code Map<SocialProviderType, SocialOAuthFlow>}로 호출.
 *
 * <p><strong>현재 한계</strong>: 신규 제공자(예: 구글) 추가 시 본 인터페이스
 * 구현체 외에도 {@link com.example.thirdtool.User.application.UserService#socialLogin}
 * 내부의 제공자별 SocialMember 저장 분기를 추가해야 한다. 본 분기는 Story 4-3
 * (`SocialMemberRegistrar` 도메인 서비스)에서 캡슐화되어 제거 예정 — 그 시점부터
 * "구현체 1개만 추가"로 완결된다.
 *
 * <p>구현체는 {@code User/infrastructure/{provider}/} 패키지에 둔다
 * (외부 OAuth API 호출의 어댑터이므로 infrastructure 계층).
 */
public interface SocialOAuthFlow {

    /**
     * 본 흐름이 담당하는 제공자 종류. Map 분기 키로 사용.
     */
    SocialProviderType getProviderType();

    /**
     * Authorization Code로 소셜 제공자에 인증을 수행하고 통일된
     * {@link SocialUserInfo}로 반환한다.
     *
     * @param code  Authorization Code (필수)
     * @param state OAuth state 파라미터 (네이버 등 필요 시. 카카오는 무시 가능)
     * @return 제공자 의존 없는 사용자 정보
     */
    SocialUserInfo authenticate(String code, String state);
}
