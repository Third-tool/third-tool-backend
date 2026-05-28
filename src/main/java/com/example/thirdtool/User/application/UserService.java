package com.example.thirdtool.User.application;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.security.auth.dto.TokenResponse;
import com.example.thirdtool.Common.security.auth.token.TokenIssuer;
import com.example.thirdtool.User.domain.exception.UserDomainException;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.model.UserRoleType;
import com.example.thirdtool.User.dto.*;
import com.example.thirdtool.User.infrastructure.Naver.NaverMember;
import com.example.thirdtool.User.infrastructure.Naver.NaverMemberRepository;
import com.example.thirdtool.User.infrastructure.kakao.KakaoMemberRepository;
import com.example.thirdtool.User.domain.repository.UserRepository;
import com.example.thirdtool.User.infrastructure.kakao.KakaoMember;

import com.example.thirdtool.Common.security.auth.jwt.JwtService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenIssuer tokenIssuer;
    private final KakaoMemberRepository kakaoMemberRepository;
    private final NaverMemberRepository naverMemberRepository;

    public UserService(PasswordEncoder passwordEncoder,
                       UserRepository userRepository,
                       JwtService jwtService,
                       TokenIssuer tokenIssuer,
                       KakaoMemberRepository kakaoMemberRepository,
                       NaverMemberRepository naverMemberRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.tokenIssuer = tokenIssuer;
        this.kakaoMemberRepository = kakaoMemberRepository;
        this.naverMemberRepository = naverMemberRepository;
    }


    // 자체 로그인 회원 가입 (존재 여부)
    @Transactional(readOnly = true)
    public Boolean existUser(UserExistRequestDTO dto) {
        return userRepository.existsByUsername(dto.getUsername());
    }

    // 자체 로그인 회원 가입
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

    // ✅ JWT 기반 자체 로그인 처리
    // 보안: "사용자 없음"과 "비밀번호 불일치"는 외부에 동일 응답(PASSWORD_NOT_MATCHED, 401)으로 통일.
    //       HTTP status code/code/message 모두 동일하게 노출해 enumeration attack 차단.
    // 운영: IS_SOCIAL(소셜 계정 자체 로그인) / USER_LOCKED(잠긴 계정)은 별도 ErrorCode로 분기.
    //       이 둘은 본인이 자기 계정 상태로 즉시 인지 가능한 정보라 누출 위험이 낮고 UX·운영에 필요.
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

    // 자체 로그인 회원 정보 수정
    @Transactional
    public Long updateUser(String username, UserUpdateRequestDTO dto) throws AccessDeniedException {
        UserEntity entity = userRepository.findByUsernameAndIsLockAndIsSocial(username, false, false)
                                          .orElseThrow(() -> UserDomainException.of(ErrorCode.USER_NOT_FOUND));

        // 수정 권한 검증은 컨트롤러 또는 서비스 진입 전에 처리
        // if (!username.equals(entity.getUsername())) {
        //     throw new AccessDeniedException("본인 계정만 수정 가능");
        // }

        entity.updateUser(dto);
        return userRepository.save(entity).getId();
    }

    @Transactional
    public void deleteUser(String username, UserDeleteRequestDTO dto) throws AccessDeniedException {

        // 권한 검증 로직은 컨트롤러나 별도의 서비스에서 처리하는 것이 좋습니다.
        // 현재는 편의상 UserService에 남겨둠.
        String sessionRole = SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
        boolean isAdmin = sessionRole.equals("ROLE_" + UserRoleType.ADMIN.name());

        if (!username.equals(dto.getUsername()) && !isAdmin) {
            throw new AccessDeniedException("본인 혹은 관리자만 삭제할 수 있습니다.");
        }

        userRepository.deleteByUsername(dto.getUsername());
        jwtService.removeRefreshUser(dto.getUsername());
    }


    // ✅ JWT 기반 소셜 로그인 처리 및 토큰 발급
    @Transactional
    public TokenResponse socialLogin(SocialProviderType socialType,
                                     String socialId,
                                     String nickname,
                                     String email,
                                     jakarta.servlet.http.HttpServletResponse response) {

        String username = socialType.name().toUpperCase() + "_" + socialId;

        UserEntity user = userRepository.findByUsername(username)
                                        .orElseGet(() -> {
                                            // 사용자가 존재하지 않으면 새로 등록
                                            UserEntity newUser = UserEntity.ofSocial(username, socialType, nickname, email);
                                            UserEntity savedUser = userRepository.save(newUser);

                                            // 소셜 멤버 정보 저장
                                            if (socialType == SocialProviderType.KAKAO) {
                                                kakaoMemberRepository.save(KakaoMember.builder().user(savedUser).kakaoId(socialId).build());
                                            } else if (socialType == SocialProviderType.NAVER) {
                                                naverMemberRepository.save(NaverMember.builder().user(savedUser).naverId(socialId).build());
                                            }
                                            return savedUser;
                                        });

        // AT는 Set-Cookie, RT는 응답 바디로 발급
        String refreshToken = tokenIssuer.issue(user, response);
        return new TokenResponse(refreshToken);
    }

    // 자체/소셜 유저 정보 조회
    @Transactional(readOnly = true)
    public UserResponseDTO readUser(String username) {
        UserEntity entity = userRepository.findByUsernameAndIsLock(username, false)
                                          .orElseThrow(() -> UserDomainException.of(ErrorCode.USER_NOT_FOUND));
        return new UserResponseDTO(username, entity.getIsSocial(), entity.getNickname(), entity.getEmail());
    }

}
