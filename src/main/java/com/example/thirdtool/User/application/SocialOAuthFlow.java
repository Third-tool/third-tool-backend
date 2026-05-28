package com.example.thirdtool.User.application;

import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.SocialUserInfo;

/**
 * 소셜 OAuth 인증 흐름의 application 포트.
 *
 * <p>Story-4-2: SocialLoginController가 제공자별 분기(switch) 없이
 * {@code Map<SocialProviderType, SocialOAuthFlow>}로 호출.
 *
 * <p>Story-4-3에서 {@code SocialMemberRegistrar} + {@code SocialMemberFactory}로
 * SocialMember 등록 분기까지 캡슐화 완료. 신규 제공자(예: 구글) 추가는 다음
 * 두 구현체를 {@code @Component}로 등록하는 것만으로 완결된다:
 * <ul>
 *   <li>본 인터페이스 {@code SocialOAuthFlow} 구현 — Authorization Code → SocialUserInfo</li>
 *   <li>{@link com.example.thirdtool.User.application.SocialMemberFactory} 구현 — SocialUserInfo → SocialMember 저장</li>
 * </ul>
 * UserService / SocialLoginController / Registrar는 무변경.
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
