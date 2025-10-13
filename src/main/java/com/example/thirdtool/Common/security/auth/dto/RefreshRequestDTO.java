package com.example.thirdtool.Common.security.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RefreshRequestDTO {

    @NotBlank
    private String refreshToken;

    public RefreshRequestDTO(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

