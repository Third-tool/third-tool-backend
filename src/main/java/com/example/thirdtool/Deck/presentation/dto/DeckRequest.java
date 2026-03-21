package com.example.thirdtool.Deck.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeckRequest {

    public record Create(
            @NotBlank
            @Size(max = 100)
            String name,

            Long parentDeckId     // null = 루트 덱
    ) {}

    public record UpdateName(
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 100)
            String name
    ) {}

    public record ChangeParent(
            Long parentDeckId       // null = 루트 덱으로 이동
    ) {}
}
