package com.example.thirdtool.User.presentation;

import com.example.thirdtool.Common.security.auth.dto.TokenResponse;
import com.example.thirdtool.Common.security.auth.token.TokenIssuer;
import com.example.thirdtool.User.application.UserCommandService;
import com.example.thirdtool.User.application.UserQueryService;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.model.UserRoleType;
import com.example.thirdtool.User.dto.*;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final TokenIssuer tokenIssuer;

    public UserController(UserCommandService userCommandService,
                          UserQueryService userQueryService,
                          TokenIssuer tokenIssuer) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
        this.tokenIssuer = tokenIssuer;
    }

    // ✅ 자체 로그인 (AT Cookie + RT Body 발급)
    // Story-5-3: 인증 컨텍스트 명시 청소 코드 제거 — STATELESS 세션이라 불필요.
    @PostMapping(value = "/login")
    public ResponseEntity<TokenResponse> loginLocal(@RequestBody LoginRequestDTO dto,
                                                    HttpServletResponse response) {
        UserEntity user = userCommandService.loginLocal(dto.getUsername(), dto.getPassword());
        String refreshToken = tokenIssuer.issue(user, response);
        return ResponseEntity.ok(new TokenResponse(refreshToken));
    }

    // 자체 로그인 유저 존재 확인 (Query)
    @PostMapping(value = "/user/exist")
    public ResponseEntity<Boolean> existUserApi(
            @Validated @RequestBody UserExistRequestDTO dto
                                               ) {
        return ResponseEntity.ok(userQueryService.existUser(dto));
    }

    // 회원가입 (Command)
    @PostMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> joinApi(
            @Validated @RequestBody UserSignUpRequestDTO dto
                                                    ) {
        Long id = userCommandService.addUser(dto);
        Map<String, Long> responseBody = Collections.singletonMap("userEntityId", id);
        return ResponseEntity.status(201).body(responseBody);
    }

    // ✅ 유저 정보 (Query)
    @GetMapping(value = "/user")
    public UserResponseDTO userMeApi(@AuthenticationPrincipal UserEntity user) {
        return userQueryService.readUser(user.getUsername());
    }

    // ✅ 유저 수정 (자체 로그인 유저만) (Command)
    // Story-5-2: @AuthenticationPrincipal을 UserEntity 단일 타입으로 통일.
    // Story-5-3: 본인 검증을 Controller에서 수행. Service는 순수 비즈니스 로직만.
    @PutMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> updateUserApi(
            @AuthenticationPrincipal UserEntity currentUser,
            @Validated @RequestBody UserUpdateRequestDTO dto
                                             ) throws AccessDeniedException {
        // 본인 검증: dto에 username이 있으면 currentUser와 일치해야 함 (Story-5-4에서 dto.username 제거 예정).
        if (dto.getUsername() != null && !currentUser.getUsername().equals(dto.getUsername())) {
            throw new AccessDeniedException("본인 계정만 수정할 수 있습니다.");
        }
        return ResponseEntity.status(200).body(userCommandService.updateUser(currentUser, dto));
    }

    // ✅ 유저 제거 (자체/소셜) (Command)
    // Story-5-3: 본인 / 관리자 권한 검증을 Controller에서 수행. Service는 순수 삭제만.
    @DeleteMapping(value = "/user")
    public ResponseEntity<Boolean> deleteUserApi(
            @AuthenticationPrincipal UserEntity currentUser,
            @Validated @RequestBody UserDeleteRequestDTO dto
                                                ) throws AccessDeniedException {
        boolean isAdmin = currentUser.getRoleType() == UserRoleType.ADMIN;
        boolean isSelfDelete = currentUser.getUsername().equals(dto.getUsername());
        if (!isSelfDelete && !isAdmin) {
            throw new AccessDeniedException("본인 혹은 관리자만 삭제할 수 있습니다.");
        }

        userCommandService.deleteUser(dto);
        return ResponseEntity.status(200).body(true);
    }
}
