package com.example.thirdtool.Deck.presentation.dto;

public class DeckRequest {

    // Create record 폐기: Fix — Axis↔Deck 완전 통합 (BE-Story 2, 2026-07-01).
    // POST /api/v1/decks 엔드포인트 자체가 사라졌으므로 관련 요청 record도 제거됨.

    public record UpdateName(
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 100)
            String name
    ) {}

    public record ChangeParent(
            Long parentDeckId       // null = 루트 덱으로 이동
    ) {}
}
