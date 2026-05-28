package com.example.thirdtool.User.application;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.security.auth.dto.TokenResponse;
import com.example.thirdtool.Common.security.auth.jwt.JwtService;
import com.example.thirdtool.Common.security.auth.token.TokenIssuer;
import com.example.thirdtool.User.domain.exception.UserDomainException;
import com.example.thirdtool.User.domain.model.SocialMemberRegistrar;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.SocialUserInfo;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.model.UserRoleType;
import com.example.thirdtool.User.domain.repository.UserRepository;
import com.example.thirdtool.User.dto.UserDeleteRequestDTO;
import com.example.thirdtool.User.dto.UserSignUpRequestDTO;
import com.example.thirdtool.User.dto.UserUpdateRequestDTO;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User BC Command Service — 상태 변경(쓰기) 유스케이스 5종.
 *
 * <p>Story-4-4: 기존 단일 {@code UserService}를 Command/Query로 분리. 본 클래스는
 * Command 측. 모든 메서드는 {@link Transactional}로 감싸여 비즈니스 단위 트랜잭션을
 * 형성한다.
 *
 * <p>책임 범위:
 * <ul>
 *   <li>{@link #addUser} — 자체 로그인 회원가입</li>
 *   <li>{@link #loginLocal} — 자체 로그인 인증 (실패 사유 코드 분기)</li>
 *   <li>{@link #updateUser} — 자체 사용자 정보 수정</li>
 *   <li>{@link #deleteUser} — 사용자 제거 + RT 화이트리스트 해제</li>
 *   <li>{@link #socialLogin} — 소셜 로그인 또는 가입 + 토큰 발급</li>
 * </ul>
 */
@Service
public class UserCommandService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenIssuer tokenIssuer;
    private final SocialMemberRegistrar socialMemberRegistrar;

    public UserCommandService(PasswordEncoder passwordEncoder,
                              UserRepository userRepository,
                              JwtService jwtService,
                              TokenIssuer tokenIssuer,
                              SocialMemberRegistrar socialMemberRegistrar) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.tokenIssuer = tokenIssuer;
        this.socialMemberRegistrar = socialMemberRegistrar;
    }

    @Transactional
    public Long addUser(UserSignUpRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw UserDomainException.of(ErrorCode.USER_ALREADY_EXISTS);
        }
        UserEntity entity = UserEntity.ofLocal(
                dto.getUsername(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getNickname(),
                dto.getEmail()
        );
        return userRepository.save(entity).getId();
    }

    /**
     * JWT 기반 자체 로그인.
     *
     * <p>보안 정책: "사용자 없음"과 "비밀번호 불일치"는 외부에 동일 응답
     * (PASSWORD_NOT_MATCHED, 401)으로 통일. HTTP status code / code / message
     * 모두 동일하게 노출해 enumeration attack 차단.
     *
     * <p>운영 정보: IS_SOCIAL(소셜 계정 자체 로그인) / USER_LOCKED(잠긴 계정)은
     * 별도 ErrorCode로 분기. 본인이 자기 계정 상태로 즉시 인지 가능한 정보라
     * 누출 위험이 낮고 UX·운영에 필요.
     */
    @Transactional
    public UserEntity loginLocal(String username, String password) {
        UserEntity user = userRepository.findByUsername(username)
                                        .orElseThrow(() -> UserDomainException.of(ErrorCode.PASSWORD_NOT_MATCHED));

        if (Boolean.TRUE.equals(user.getIsSocial())) {
            throw UserDomainException.of(ErrorCode.USER_IS_SOCIAL);
        }
        if (Boolean.TRUE.equals(user.getIsLock())) {
            throw UserDomainException.of(ErrorCode.USER_LOCKED);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw UserDomainException.of(ErrorCode.PASSWORD_NOT_MATCHED);
        }
        return user;
    }

    @Transactional
    public Long updateUser(UserEntity currentUser, UserUpdateRequestDTO dto) throws AccessDeniedException {
        // Story-5-2 본인 검증: 요청 바디의 username이 currentUser와 다르면 타 계정 수정 시도로 차단.
        // (UserUpdateRequestDTO.username 자체 제거는 Story-5-4 범위)
        if (dto.getUsername() != null && !currentUser.getUsername().equals(dto.getUsername())) {
            throw new AccessDeniedException("본인 계정만 수정할 수 있습니다.");
        }

        // currentUser 정보로 자체 로그인·non-lock 조건을 만족하는 사용자를 찾아 수정.
        UserEntity entity = userRepository.findByUsernameAndIsLockAndIsSocial(currentUser.getUsername(), false, false)
                                          .orElseThrow(() -> UserDomainException.of(ErrorCode.USER_NOT_FOUND));
        entity.updateUser(dto);
        return userRepository.save(entity).getId();
    }

    @Transactional
    public void deleteUser(UserEntity currentUser, UserDeleteRequestDTO dto) throws AccessDeniedException {
        // Story-5-2: SecurityContextHolder 직접 호출 제거 → currentUser.getRoleType() 활용.
        // 권한 검증을 @PreAuthorize로 이관하는 것은 Story-5-3 범위 (현재는 Service에서 처리).
        boolean isAdmin = currentUser.getRoleType() == UserRoleType.ADMIN;
        boolean isSelfDelete = currentUser.getUsername().equals(dto.getUsername());

        if (!isSelfDelete && !isAdmin) {
            throw new AccessDeniedException("본인 혹은 관리자만 삭제할 수 있습니다.");
        }

        // 본인 삭제는 currentUser 기반, admin 삭제는 dto.username 기반 (admin이 다른 계정 삭제).
        // 두 케이스 모두 결과 username은 dto.username과 동일 (isSelfDelete=true일 때 currentUser.username == dto.username).
        String targetUsername = isSelfDelete ? currentUser.getUsername() : dto.getUsername();
        userRepository.deleteByUsername(targetUsername);
        jwtService.removeRefreshUser(targetUsername);
    }

    /**
     * 소셜 로그인 + (필요 시) 자동 가입.
     *
     * <p>Spring {@link Transactional}이 본 메서드를 감싼다 — {@link SocialMemberRegistrar#register}
     * 예외 시 UserEntity 저장 포함 전체 롤백 (Story-4-3 AC3).
     */
    @Transactional
    public TokenResponse socialLogin(SocialProviderType socialType,
                                     String socialId,
                                     String nickname,
                                     String email,
                                     jakarta.servlet.http.HttpServletResponse response) {

        String username = socialType.name().toUpperCase() + "_" + socialId;

        UserEntity user = userRepository.findByUsername(username)
                                        .orElseGet(() -> {
                                            UserEntity newUser = UserEntity.ofSocial(username, socialType, nickname, email);
                                            UserEntity savedUser = userRepository.save(newUser);
                                            socialMemberRegistrar.register(
                                                    savedUser,
                                                    new SocialUserInfo(socialId, nickname, email, socialType)
                                            );
                                            return savedUser;
                                        });

        String refreshToken = tokenIssuer.issue(user, response);
        return new TokenResponse(refreshToken);
    }
}
