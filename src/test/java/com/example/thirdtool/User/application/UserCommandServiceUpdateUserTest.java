package com.example.thirdtool.User.application;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.security.auth.jwt.JwtService;
import com.example.thirdtool.Common.security.auth.token.TokenIssuer;
import com.example.thirdtool.User.domain.exception.UserDomainException;
import com.example.thirdtool.User.domain.model.SocialMemberRegistrar;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import com.example.thirdtool.User.dto.UserUpdateRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Story-5-4: UserCommandService.updateUser 단위 — 수정 대상자는 currentUser로만 결정,
 * DTO는 nickname/email 2 필드만. Classist (Repository만 Mock, UserEntity 실제).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserCommandService.updateUser")
class UserCommandServiceUpdateUserTest {

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
    private UserCommandService userCommandService;

    private UserEntity currentUser;

    @BeforeEach
    void setUp() {
        currentUser = UserEntity.ofLocal("user@example.com", "encoded-pw", "기존닉네임", "old@example.com");
        ReflectionTestUtils.setField(currentUser, "id", 42L);
    }

    @Test
    @DisplayName("정상 dto면 email·nickname을 entity에 반영하고 id를 반환한다")
    void 정상_dto면_email_nickname을_entity에_반영하고_id를_반환한다() {
        UserUpdateRequestDTO dto = new UserUpdateRequestDTO("새닉네임", "new@example.com");
        given(userRepository.findByUsernameAndIsLockAndIsSocial("user@example.com", false, false))
                .willReturn(Optional.of(currentUser));
        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> inv.getArgument(0));

        Long resultId = userCommandService.updateUser(currentUser, dto);

        assertThat(resultId).isEqualTo(42L);
        assertThat(currentUser.getEmail()).isEqualTo("new@example.com");
        assertThat(currentUser.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("currentUser.username으로 repository 조회 — DTO에 username 필드가 없음을 컴파일 + 동작으로 보장")
    void currentUser_username으로_repository_조회() {
        UserUpdateRequestDTO dto = new UserUpdateRequestDTO("nick", "x@y.com");
        given(userRepository.findByUsernameAndIsLockAndIsSocial("user@example.com", false, false))
                .willReturn(Optional.of(currentUser));
        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> inv.getArgument(0));

        userCommandService.updateUser(currentUser, dto);

        // 본 메서드가 dto의 username 같은 필드를 참조하지 않고 currentUser.getUsername()만 사용함을
        // 회귀 안전망으로 보장 — DTO에 username 필드가 다시 추가되어도 본 테스트는 통과하지만,
        // 핵심은 DTO에 그 필드가 존재하지 않는다는 사실(컴파일 시점 보장).
        assertThat(currentUser.getUsername()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("dto.email = null이면 entity.email도 null로 변경된다 (덮어쓰기)")
    void dto_email이_null이면_entity_email도_null로_변경된다() {
        UserUpdateRequestDTO dto = new UserUpdateRequestDTO("새닉네임", null);
        given(userRepository.findByUsernameAndIsLockAndIsSocial("user@example.com", false, false))
                .willReturn(Optional.of(currentUser));
        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> inv.getArgument(0));

        userCommandService.updateUser(currentUser, dto);

        assertThat(currentUser.getEmail()).isNull();
        assertThat(currentUser.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("dto.nickname = null이면 entity.nickname도 null로 변경된다 (덮어쓰기)")
    void dto_nickname이_null이면_entity_nickname도_null로_변경된다() {
        UserUpdateRequestDTO dto = new UserUpdateRequestDTO(null, "new@example.com");
        given(userRepository.findByUsernameAndIsLockAndIsSocial("user@example.com", false, false))
                .willReturn(Optional.of(currentUser));
        given(userRepository.save(any(UserEntity.class))).willAnswer(inv -> inv.getArgument(0));

        userCommandService.updateUser(currentUser, dto);

        assertThat(currentUser.getNickname()).isNull();
        assertThat(currentUser.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("repository에 currentUser.username 매칭 entity 없으면 USER_NOT_FOUND throw")
    void repository에_currentUser_username_매칭_entity_없으면_USER_NOT_FOUND_throw() {
        UserUpdateRequestDTO dto = new UserUpdateRequestDTO("nick", "x@y.com");
        given(userRepository.findByUsernameAndIsLockAndIsSocial("user@example.com", false, false))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> userCommandService.updateUser(currentUser, dto))
                .isInstanceOf(UserDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
