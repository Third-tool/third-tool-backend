package com.example.thirdtool.User.application;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.User.domain.exception.UserDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import com.example.thirdtool.User.dto.UserExistRequestDTO;
import com.example.thirdtool.User.dto.UserResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User BC Query Service — 조회 전용 유스케이스.
 *
 * <p>Story-4-4: 기존 단일 {@code UserService}를 Command/Query로 분리. 본 클래스는
 * Query 측. 클래스 레벨 {@link Transactional}(readOnly=true)로 dirty checking을
 * 차단해 의도치 않은 UPDATE 쿼리 방지 (AC4).
 *
 * <p>책임 범위:
 * <ul>
 *   <li>{@link #existUser} — 자체 로그인 회원가입 전 username 중복 확인</li>
 *   <li>{@link #readUser} — 자체/소셜 사용자 프로필 조회</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;

    public UserQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Boolean existUser(UserExistRequestDTO dto) {
        return userRepository.existsByUsername(dto.getUsername());
    }

    public UserResponseDTO readUser(String username) {
        UserEntity entity = userRepository.findByUsernameAndIsLock(username, false)
                                          .orElseThrow(() -> UserDomainException.of(ErrorCode.USER_NOT_FOUND));
        return new UserResponseDTO(username, entity.getIsSocial(), entity.getNickname(), entity.getEmail());
    }
}
