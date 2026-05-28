package com.example.thirdtool.User.application;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.security.auth.jwt.JwtService;
import com.example.thirdtool.Common.security.auth.token.TokenIssuer;
import com.example.thirdtool.User.domain.exception.UserDomainException;
import com.example.thirdtool.User.domain.model.SocialMemberRegistrar;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.model.UserRoleType;
import com.example.thirdtool.User.domain.repository.UserRepository;
import com.example.thirdtool.User.dto.UserSignUpRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Story-5-1: UserService 예외 통일 검증.
 *
 * loginLocal()의 실패 사유별 ErrorCode 분기와 addUser() 중복 가입 시
 * USER_ALREADY_EXISTS throw를 단위로 확인한다. 단위 테스트라 Repository와
 * PasswordEncoder만 Mock — 도메인 객체(UserEntity)는 실제 인스턴스 사용
 * (conventions.md §4.1 Classist + Mock 외부 의존만).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceLoginExceptionTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private TokenIssuer tokenIssuer;
    @Mock
    private SocialMemberRegistrar socialMemberRegistrar;

    @InjectMocks
    private UserService userService;

    private UserEntity localUser;
    private UserEntity socialUser;
    private UserEntity lockedUser;

    @BeforeEach
    void setUp() {
        localUser = UserEntity.ofLocal("alice", "ENCODED_PWD", "Alice", "alice@example.com");
        socialUser = UserEntity.ofSocial("KAKAO_12345", SocialProviderType.KAKAO, "Kakao Alice", "kakao@example.com");
        lockedUser = UserEntity.of(
                "bob", "ENCODED_PWD",
                true, false,
                null, UserRoleType.USER,
                "Bob", "bob@example.com");
    }

    @Test
    void loginLocal_해피_정상_사용자_반환() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(localUser));
        given(passwordEncoder.matches("plain", "ENCODED_PWD")).willReturn(true);

        UserEntity result = userService.loginLocal("alice", "plain");

        assertThat(result).isSameAs(localUser);
    }

    @Test
    void loginLocal_사용자없음_PASSWORD_NOT_MATCHED로_통일() {
        // 보안 정책(Critical 1 조치): 사용자 없음 / 비밀번호 불일치는 외부 응답 동일.
        // 둘 다 PASSWORD_NOT_MATCHED(401)로 묶어 enumeration attack 차단.
        given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loginLocal("ghost", "pwd"))
                .isInstanceOf(UserDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_NOT_MATCHED);
    }

    @Test
    void loginLocal_소셜유저_USER_IS_SOCIAL() {
        given(userRepository.findByUsername("KAKAO_12345")).willReturn(Optional.of(socialUser));

        assertThatThrownBy(() -> userService.loginLocal("KAKAO_12345", "pwd"))
                .isInstanceOf(UserDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_IS_SOCIAL);
    }

    @Test
    void loginLocal_잠긴계정_USER_LOCKED() {
        given(userRepository.findByUsername("bob")).willReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> userService.loginLocal("bob", "pwd"))
                .isInstanceOf(UserDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_LOCKED);
    }

    @Test
    void loginLocal_비밀번호불일치_PASSWORD_NOT_MATCHED() {
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(localUser));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        assertThatThrownBy(() -> userService.loginLocal("alice", "wrong"))
                .isInstanceOf(UserDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_NOT_MATCHED);
    }

    @Test
    void addUser_중복가입_USER_ALREADY_EXISTS() {
        given(userRepository.existsByUsername("alice")).willReturn(true);

        UserSignUpRequestDTO dto = new UserSignUpRequestDTO();
        // dto는 getter만 사용되므로 reflection 없이 mockito BDD 스타일로 처리
        // 다만 UserSignUpRequestDTO가 lombok @Setter라면 setter, 아니면 reflection 필요.
        // 본 테스트는 existsByUsername 체크 단계까지만 도달하므로 username 한 필드만 채워도 OK.
        org.springframework.test.util.ReflectionTestUtils.setField(dto, "username", "alice");

        assertThatThrownBy(() -> userService.addUser(dto))
                .isInstanceOf(UserDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
    }
}
