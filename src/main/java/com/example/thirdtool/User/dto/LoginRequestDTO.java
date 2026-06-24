package com.example.thirdtool.User.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    /**
     * Story 3-1: 비밀번호 원문이 로그·예외 메시지·디버거 출력에 노출되지 않도록 toString을 명시 오버라이드.
     * Validation 실패 시 Spring이 DTO를 toString해 로그에 흘리는 경로(MethodArgumentNotValidException)
     * 등 모든 직렬화 경로에서 password 자리를 "***"로 고정.
     */
    @Override
    public String toString() {
        return "LoginRequestDTO(username=" + username + ", password=***)";
    }
}