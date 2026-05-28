package com.example.thirdtool.User.application;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.User.domain.exception.UserDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import com.example.thirdtool.User.dto.UserExistRequestDTO;
import com.example.thirdtool.User.dto.UserResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * Story-4-4: UserQueryService 단위 검증.
 *
 * 조회 전용 서비스라 트랜잭션 readOnly 동작은 슬라이스/통합 테스트가 더 적절하나,
 * 본 단위는 입출력 매핑·예외 분기만 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserQueryService userQueryService;

    @Test
    void existUser_username_존재_true_반환() {
        given(userRepository.existsByUsername("alice")).willReturn(true);
        UserExistRequestDTO dto = new UserExistRequestDTO();
        ReflectionTestUtils.setField(dto, "username", "alice");

        Boolean result = userQueryService.existUser(dto);

        assertThat(result).isTrue();
    }

    @Test
    void existUser_username_미존재_false_반환() {
        given(userRepository.existsByUsername("ghost")).willReturn(false);
        UserExistRequestDTO dto = new UserExistRequestDTO();
        ReflectionTestUtils.setField(dto, "username", "ghost");

        Boolean result = userQueryService.existUser(dto);

        assertThat(result).isFalse();
    }

    @Test
    void readUser_정상사용자_UserResponseDTO_반환() {
        UserEntity entity = UserEntity.ofLocal("alice", "ENCODED", "Alice", "a@test");
        given(userRepository.findByUsernameAndIsLock("alice", false)).willReturn(Optional.of(entity));

        UserResponseDTO result = userQueryService.readUser("alice");

        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.social()).isFalse();
        assertThat(result.nickname()).isEqualTo("Alice");
        assertThat(result.email()).isEqualTo("a@test");
    }

    @Test
    void readUser_사용자없음_USER_NOT_FOUND() {
        given(userRepository.findByUsernameAndIsLock("ghost", false)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.readUser("ghost"))
                .isInstanceOf(UserDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
