package com.example.thirdtool.User.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {

    // 회원 가입 및 로그인 공통
    @NotBlank
    @Size(min = 4)
    private String username;

    @NotBlank(message = "비밀번호는 최소 4자리 이상이어야 합니다.")
    @Size(min = 4)
    private String password;

    // 회원 정보 (가입 + 수정)
    private String nickname;

    @Email
    private String email;

    // 소셜 로그인 가입 시 활용
    private Boolean isSocial;
    private String provider; // "NAVER", "KAKAO"
}