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
public class UserUpdateRequestDTO {
    @NotBlank
    @Size(min = 4)
    private String username;

    private String password; // 비밀번호 변경이 있을 수도, 없을 수도 있음

    private String nickname;

    @Email
    private String email;
}