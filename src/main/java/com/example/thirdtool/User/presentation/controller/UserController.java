package com.example.thirdtool.User.presentation.controller;

import com.example.thirdtool.User.application.service.UserService;
import com.example.thirdtool.User.domain.model.User;
import com.example.thirdtool.User.presentation.dto.UserJoinRequestDto;
import com.example.thirdtool.User.presentation.dto.UserLoginRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/join")
    public ResponseEntity<User> join(@Valid @RequestBody UserJoinRequestDto dto) {
        User newUser = userService.join(dto);
        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody UserLoginRequestDto dto) {
        User user = userService.login(dto);
        return ResponseEntity.ok("로그인 성공! 유저 ID: " + user.getId());
    }
}