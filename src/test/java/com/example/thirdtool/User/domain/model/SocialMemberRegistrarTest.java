package com.example.thirdtool.User.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.User.application.SocialMemberFactory;
import com.example.thirdtool.User.domain.exception.UserDomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Story-4-3: SocialMemberRegistrar 도메인 서비스 — 제공자별 Factory dispatch +
 * 중복 검증(SOCIAL_MEMBER_ALREADY_LINKED).
 *
 * Classist 전략 (도메인 서비스 본체는 실제 인스턴스) + Factory만 Mock.
 */
class SocialMemberRegistrarTest {

    private SocialMemberFactory kakaoFactory;
    private SocialMemberFactory naverFactory;
    private SocialMemberRegistrar registrar;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        kakaoFactory = Mockito.mock(SocialMemberFactory.class);
        naverFactory = Mockito.mock(SocialMemberFactory.class);
        // getProviderType은 Registrar 생성자에서만 호출 — lenient로 unused stub 경고 회피
        // (partial registrar 케이스에서 naverFactory가 사용 안 됨)
        lenient().when(kakaoFactory.getProviderType()).thenReturn(SocialProviderType.KAKAO);
        lenient().when(naverFactory.getProviderType()).thenReturn(SocialProviderType.NAVER);

        registrar = new SocialMemberRegistrar(List.of(kakaoFactory, naverFactory));

        user = UserEntity.ofSocial("KAKAO_12345", SocialProviderType.KAKAO, "Alice", "a@k.test");
    }

    @Test
    void register_KAKAO_정상_KakaoFactory_위임() {
        given(kakaoFactory.existsBySocialId("12345")).willReturn(false);
        SocialUserInfo info = new SocialUserInfo("12345", "Alice", "a@k.test", SocialProviderType.KAKAO);

        registrar.register(user, info);

        verify(kakaoFactory).register(user, info);
        verify(naverFactory, never()).register(any(), any());
    }

    @Test
    void register_NAVER_정상_NaverFactory_위임() {
        given(naverFactory.existsBySocialId("naver-999")).willReturn(false);
        SocialUserInfo info = new SocialUserInfo("naver-999", "Bob", "b@n.test", SocialProviderType.NAVER);

        registrar.register(user, info);

        verify(naverFactory).register(user, info);
        verify(kakaoFactory, never()).register(any(), any());
    }

    @Test
    void register_중복socialId_SOCIAL_MEMBER_ALREADY_LINKED() {
        given(kakaoFactory.existsBySocialId("12345")).willReturn(true);
        SocialUserInfo info = new SocialUserInfo("12345", "Alice", "a@k.test", SocialProviderType.KAKAO);

        assertThatThrownBy(() -> registrar.register(user, info))
                .isInstanceOf(UserDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOCIAL_MEMBER_ALREADY_LINKED);

        // 중복 시 실제 저장은 호출되지 않음
        verify(kakaoFactory, never()).register(any(), any());
    }

    @Test
    void constructor_같은Provider_Factory_중복등록시_부팅실패() {
        SocialMemberFactory duplicateKakao = Mockito.mock(SocialMemberFactory.class);
        given(duplicateKakao.getProviderType()).willReturn(SocialProviderType.KAKAO);

        assertThatThrownBy(() -> new SocialMemberRegistrar(List.of(kakaoFactory, duplicateKakao)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate SocialMemberFactory");
    }

    @Test
    void register_등록안된Provider_SOCIAL_PROVIDER_NOT_SUPPORTED() {
        // KAKAO Factory만 등록한 상태에서 NAVER 요청이 들어오는 시나리오.
        // 향후 SocialProviderType enum에 새 값을 추가했는데 SocialMemberFactory 구현체를
        // @Component로 등록하지 않은 회귀 케이스를 재현.
        SocialMemberRegistrar partialRegistrar = new SocialMemberRegistrar(List.of(kakaoFactory));
        SocialUserInfo info = new SocialUserInfo("naver-999", "Bob", "b@n.test", SocialProviderType.NAVER);

        assertThatThrownBy(() -> partialRegistrar.register(user, info))
                .isInstanceOf(UserDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
    }
}
