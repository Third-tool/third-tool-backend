package com.example.thirdtool.User.presentation;

import com.example.thirdtool.Common.security.auth.dto.TokenResponse;
import com.example.thirdtool.Common.security.auth.token.TokenIssuer;
import com.example.thirdtool.User.application.UserCommandService;
import com.example.thirdtool.User.application.UserQueryService;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.dto.*;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @PostMapping(value = "/login")
    public ResponseEntity<TokenResponse> loginLocal(@RequestBody LoginRequestDTO dto,
                                                    HttpServletResponse response) {

        SecurityContextHolder.clearContext();
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
    // 인증 주체 주입은 Spring Security가 JWTFilter에서 설정한 Principal을 그대로 활용.
    @PutMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> updateUserApi(
            @AuthenticationPrincipal UserEntity currentUser,
            @Validated @RequestBody UserUpdateRequestDTO dto
                                             ) throws AccessDeniedException {
        return ResponseEntity.status(200).body(userCommandService.updateUser(currentUser, dto));
    }

    // ✅ 유저 제거 (자체/소셜) (Command)
    @DeleteMapping(value = "/user")
    public ResponseEntity<Boolean> deleteUserApi(
            @AuthenticationPrincipal UserEntity currentUser,
            @Validated @RequestBody UserDeleteRequestDTO dto
                                                ) throws AccessDeniedException {
        userCommandService.deleteUser(currentUser, dto);
        return ResponseEntity.status(200).body(true);
    }
}
