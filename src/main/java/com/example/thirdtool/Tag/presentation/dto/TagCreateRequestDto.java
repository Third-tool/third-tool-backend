package com.example.thirdtool.Tag.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record TagCreateRequestDto(@NotBlank String name) {

}
