package com.example.thirdtool.Card.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class CardRequest {

    public record Create(
            MainNoteDto mainNote,

            @NotEmpty
            List<String> keywords,

            @NotBlank
            String summary
    ) {}

    public record UpdateMainNote(
            String textContent,
            String imageUrl
    ) {}

    public record UpdateSummary(
            @NotBlank
            String summary
    ) {}

    public record ReplaceKeywords(
            @NotEmpty
            List<String> keywords
    ) {}

    public record AddKeyword(
            @NotBlank
            String value
    ) {}

    // ─── 중첩 DTO ─────────────────────────────────────────
    public record MainNoteDto(
            String textContent,
            String imageUrl
    ) {}
}

