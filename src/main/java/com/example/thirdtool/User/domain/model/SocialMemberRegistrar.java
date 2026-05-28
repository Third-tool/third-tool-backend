package com.example.thirdtool.User.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.User.application.SocialMemberFactory;
import com.example.thirdtool.User.domain.exception.UserDomainException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 소셜 회원 등록 도메인 서비스.
 *
 * <p>Story-4-3: {@code UserService}가 카카오·네이버 Repository를 직접 알지 않도록
 * 제공자별 {@link SocialMemberFactory} 호출을 캡슐화. {@code Map<SocialProviderType,
 * SocialMemberFactory>} 자동 dispatch 패턴은 Story-4-2의 {@code SocialOAuthFlow}와 동일.
 *
 * <p>도메인 서비스 위치 근거: Card BC의 {@code CardStatusHistoryAppender} 패턴 답습.
 * Aggregate가 직접 Repository를 호출하지 않고, 다중 협력이 필요한 영속 책임을
 * 도메인 서비스로 위임 (CLAUDE.md §1.7 — "Domain Service 호출은 Application Service가 한다").
 *
 * <p>중복 검증: {@code SocialMember.socialId}는 DB 레벨 unique 제약 + 본 서비스의
 * 사전 검증으로 이중 방어 (conventions.md §1.6).
 */
@Component
public class SocialMemberRegistrar {

    private final Map<SocialProviderType, SocialMemberFactory> factories;

    public SocialMemberRegistrar(List<SocialMemberFactory> factories) {
        this.factories = factories.stream()
                                  .collect(Collectors.toMap(
                                          SocialMemberFactory::getProviderType,
                                          Function.identity(),
                                          (a, b) -> {
                                              throw new IllegalStateException(
                                                      "Duplicate SocialMemberFactory for provider: " + a.getProviderType());
                                          }
                                  ));
    }

    /**
     * 제공자별 SocialMember를 등록한다. 이미 같은 {@code socialId}로 등록된
     * 회원이 있으면 {@code SOCIAL_MEMBER_ALREADY_LINKED}를 던진다 (AC4).
     *
     * @throws UserDomainException {@code SOCIAL_PROVIDER_NOT_SUPPORTED} — 등록된 Factory 없음
     * @throws UserDomainException {@code SOCIAL_MEMBER_ALREADY_LINKED} — 동일 socialId 중복
     */
    public void register(UserEntity user, SocialUserInfo info) {
        SocialMemberFactory factory = factories.get(info.provider());
        if (factory == null) {
            throw UserDomainException.of(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }
        if (factory.existsBySocialId(info.socialId())) {
            throw UserDomainException.of(ErrorCode.SOCIAL_MEMBER_ALREADY_LINKED);
        }
        factory.register(user, info);
    }
}
