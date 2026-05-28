package com.example.thirdtool.User.application;

import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.SocialUserInfo;
import com.example.thirdtool.User.domain.model.UserEntity;

/**
 * 제공자별 {@code SocialMember} 저장 책임을 캡슐화하는 Strategy 인터페이스.
 *
 * <p>Story-4-3: {@link com.example.thirdtool.User.domain.model.SocialMemberRegistrar}가
 * {@code Map<SocialProviderType, SocialMemberFactory>}로 dispatch. 신규 제공자
 * 추가는 본 인터페이스의 {@code @Component} 구현체 1개 추가만으로 완결
 * (Story-4-2의 `SocialOAuthFlow` 패턴과 일관).
 *
 * <p>구현체는 {@code User/infrastructure/{provider}/} 패키지에 두며, 해당
 * 제공자의 SocialMember Repository를 직접 의존한다.
 */
public interface SocialMemberFactory {

    /**
     * 본 Factory가 담당하는 제공자 종류. Map 분기 키.
     */
    SocialProviderType getProviderType();

    /**
     * 동일 {@code socialId}로 이미 등록된 SocialMember가 존재하는지 확인.
     * Registrar가 본 메서드로 중복을 검증해 {@code SOCIAL_MEMBER_ALREADY_LINKED}를 던진다.
     */
    boolean existsBySocialId(String socialId);

    /**
     * UserEntity와 SocialUserInfo로부터 제공자별 구체 {@code SocialMember}를 생성·저장한다.
     */
    void register(UserEntity user, SocialUserInfo info);
}
