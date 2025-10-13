package com.example.thirdtool.User.application;

import com.example.thirdtool.Card.application.service.CardRankService;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.security.Util.JWTUtil;
import com.example.thirdtool.User.domain.model.CustomOAuth2User;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class UserService extends DefaultOAuth2UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final CardRankService cardRankService;
    private final JwtService jwtService;
    private final KakaoMemberRepository kakaoMemberRepository;
    private final NaverMemberRepository naverMemberRepository;

    public UserService(PasswordEncoder passwordEncoder,
                       UserRepository userRepository,
                       CardRankService cardRankService,
                       JwtService jwtService,
                       KakaoMemberRepository kakaoMemberRepository,
                       NaverMemberRepository naverMemberRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.cardRankService = cardRankService;
        this.jwtService = jwtService;
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
            throw new IllegalArgumentException("이미 유저가 존재합니다.");
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
    @Transactional
    public UserEntity loginLocal(String username, String password) {
        UserEntity user = userRepository.findByUsernameAndIsLockAndIsSocial(username, false, false)
                                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        cardRankService.createDefaultRanksIfAbsent(user.getId());
        return user;
    }

    // 자체 로그인 회원 정보 수정
    @Transactional
    public Long updateUser(String username, UserUpdateRequestDTO dto) throws AccessDeniedException {
        UserEntity entity = userRepository.findByUsernameAndIsLockAndIsSocial(username, false, false)
                                          .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + username));

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
    public TokenResponse socialLogin(SocialProviderType socialType, String socialId, String nickname, String email) {

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

        // JWT(Access/Refresh) 발급
        String role = "ROLE_" + user.getRoleType().name();
        String accessToken = JWTUtil.createJWT(user.getUsername(), role, true);
        String refreshToken = JWTUtil.createJWT(user.getUsername(), role, false);

        // Refresh 토큰 DB 저장
        jwtService.addRefresh(user.getUsername(), refreshToken);

        cardRankService.createDefaultRanksIfAbsent(user.getId());

        return new TokenResponse(accessToken, refreshToken);
    }

    // 자체/소셜 유저 정보 조회
    @Transactional(readOnly = true)
    public UserResponseDTO readUser(String username) {
        UserEntity entity = userRepository.findByUsernameAndIsLock(username, false)
                                          .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + username));
        return new UserResponseDTO(username, entity.getIsSocial(), entity.getNickname(), entity.getEmail());
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "kakao", "naver"
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 공통 파싱 구조
        String socialId;
        String nickname;
        String email;

        if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            socialId = String.valueOf(attributes.get("id"));
            nickname = (String) profile.get("nickname");
            email = (String) kakaoAccount.get("email");
        } else if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            socialId = (String) response.get("id");
            nickname = (String) response.get("nickname");
            email = (String) response.get("email");
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }

        // username 규칙: PROVIDER_SOCIALID (ex. KAKAO_12345)
        String username = registrationId.toUpperCase() + "_" + socialId;

        // DB 유저 조회 또는 생성
        UserEntity user = userRepository.findByUsername(username)
                                        .orElseGet(() -> {
                                            UserEntity newUser = UserEntity.ofSocial(
                                                    username,
                                                    SocialProviderType.valueOf(registrationId.toUpperCase()),
                                                    nickname,
                                                    email
                                                                                    );
                                            return userRepository.save(newUser);
                                        });

        // 반환: UserEntity를 래핑한 CustomOAuth2User
        return new CustomOAuth2User(user, attributes);
    }


}
