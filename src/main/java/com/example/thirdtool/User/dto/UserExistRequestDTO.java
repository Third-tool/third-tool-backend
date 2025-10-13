package com.example.thirdtool.User.dto;

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
public class UserExistRequestDTO {
    @NotBlank
    @Size(min = 4, message = "아이디는 최소 4자리 이상이어야 합니다.")
    private String username;
}