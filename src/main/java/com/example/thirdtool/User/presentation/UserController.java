package com.example.thirdtool.User.presentation;

import com.example.thirdtool.Common.security.auth.dto.TokenResponse;
import com.example.thirdtool.Common.security.auth.token.TokenIssuer;
import com.example.thirdtool.User.dto.*;
import com.example.thirdtool.User.application.UserService;
import com.example.thirdtool.User.domain.model.UserEntity;

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

    private final UserService userService;
    private final TokenIssuer tokenIssuer;

    public UserController(UserService userService,
                          TokenIssuer tokenIssuer) {
        this.userService = userService;
        this.tokenIssuer = tokenIssuer;
    }

    // ✅ 자체 로그인 (AT Cookie + RT Body 발급)
    @PostMapping(value = "/login")
    public ResponseEntity<TokenResponse> loginLocal(@RequestBody LoginRequestDTO dto,
                                                    HttpServletResponse response) {

        SecurityContextHolder.clearContext();
        // 1️⃣ 유저 인증
        UserEntity user = userService.loginLocal(dto.getUsername(), dto.getPassword());

        // 2️⃣ 토큰 발급 — AT는 Set-Cookie, RT는 응답 바디
        String refreshToken = tokenIssuer.issue(user, response);

        // 3️⃣ 응답 (AT는 Cookie로 이미 전달됨)
        return ResponseEntity.ok(new TokenResponse(refreshToken));
    }

    // 자체 로그인 유저 존재 확인
    @PostMapping(value = "/user/exist")
    public ResponseEntity<Boolean> existUserApi(
            @Validated @RequestBody UserExistRequestDTO dto
                                               ) {
        return ResponseEntity.ok(userService.existUser(dto));
    }

    // 회원가입
    @PostMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> joinApi(
            @Validated @RequestBody UserSignUpRequestDTO dto
                                                    ) {
        Long id = userService.addUser(dto);
        Map<String, Long> responseBody = Collections.singletonMap("userEntityId", id);
        return ResponseEntity.status(201).body(responseBody);
    }

    // ✅ 유저 정보
    @GetMapping(value = "/user")
    public UserResponseDTO userMeApi(@AuthenticationPrincipal UserEntity user) {
        return userService.readUser(user.getUsername());
    }

    // ✅ 유저 수정 (자체 로그인 유저만)
    @PutMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> updateUserApi(
            @AuthenticationPrincipal String username,
            @Validated @RequestBody UserUpdateRequestDTO dto
                                             ) throws AccessDeniedException {
        // 서비스 메서드에 username을 넘겨 로직 처리
        return ResponseEntity.status(200).body(userService.updateUser(username, dto));
    }

    // ✅ 유저 제거 (자체/소셜)
    @DeleteMapping(value = "/user")
    public ResponseEntity<Boolean> deleteUserApi(
            @AuthenticationPrincipal String username,
            @Validated @RequestBody UserDeleteRequestDTO dto
                                                ) throws AccessDeniedException {
        // 서비스 메서드에 username을 넘겨 로직 처리
        userService.deleteUser(username, dto);
        return ResponseEntity.status(200).body(true);
    }


}
