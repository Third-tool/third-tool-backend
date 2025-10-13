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
public class UserSignUpRequestDTO {
    @NotBlank
    @Size(min = 4, message = "아이디는 최소 4자리 이상이어야 합니다.")
    private String username;

    @NotBlank
    @Size(min = 4, message = "비밀번호는 최소 4자리 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;
}
