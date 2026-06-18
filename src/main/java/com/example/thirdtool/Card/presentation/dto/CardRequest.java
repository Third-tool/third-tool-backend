package com.example.thirdtool.Card.presentation.dto;

import com.example.thirdtool.Card.domain.model.ArchiveReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CardRequest {

    // ─── 1. 카드 생성 ─────────────────────────────────────
    public record Create(
            MainNoteDto mainNote,

            @NotEmpty
            List<String> keywords,

            @NotBlank
            String summary,

            // 선택 항목. 미입력 시 빈 목록으로 처리. 최대 3개까지 허용.
            List<String> tags
    ) {}

    // ─── 4. MainNote 수정 ─────────────────────────────────
    public record UpdateMainNote(
            String textContent,
            String imageUrl
    ) {}

    // ─── 5. Summary 수정 ──────────────────────────────────
    public record UpdateSummary(
            @NotBlank
            String summary
    ) {}

    // ─── 6. Keyword 전체 교체 ─────────────────────────────
    public record ReplaceKeywords(
            @NotEmpty
            List<String> keywords
    ) {}

    // ─── 7. Keyword 단건 추가 ─────────────────────────────
    public record AddKeyword(
            @NotBlank
            String value
    ) {}

    // ─── 9. 태그 단건 추가 ────────────────────────────────
    public record AddTag(
            @NotBlank
            String value
    ) {}

    // ─── 11. 태그 전체 교체 ───────────────────────────────
    // 기존 태그 연결을 모두 제거하고 새 목록으로 교체한다.
    public record ReplaceTags(
            @NotEmpty
            List<String> tags
    ) {}

    // ─── 카드 ARCHIVE 전환 요청 ───────────────────────────
    // 사용자 명시 액션의 reason은 MANUAL로 고정 (Service에서 강제) — 본 DTO는 향후
    // 관리자/시스템 호출(MAX_VIEW·MAX_DURATION)을 위한 확장 여지를 위해 enum을 받는다.
    public record Archive(
            @NotNull
            ArchiveReason reason
    ) {}

    // ─── 중첩 DTO ─────────────────────────────────────────
    public record MainNoteDto(
            String textContent,
            String imageUrl
    ) {}
}
