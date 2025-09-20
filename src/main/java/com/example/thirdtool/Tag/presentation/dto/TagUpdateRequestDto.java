package com.example.thirdtool.Tag.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagUpdateRequestDto(@NotBlank @Size(max = 64) String name) {

}
