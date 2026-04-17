package com.example.thirdtool.LearningFacade.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class LearningFacadeRequest {

    // ─── Facade ───────────────────────────────────────────

    public record CreateFacade(
            @NotBlank
            String concept
    ) {}

    public record UpdateConcept(
            @NotBlank
            String concept
    ) {}

    // ─── Axis ─────────────────────────────────────────────

    public record AddAxis(
            @NotBlank
            String name
    ) {}

    public record UpdateAxisName(
            @NotBlank
            String name
    ) {}

    public record ReorderAxes(
            @NotEmpty
            List<Long> orderedAxisIds
    ) {}

    // ─── Action ───────────────────────────────────────────

    public record AddAction(
            @NotBlank
            String description
    ) {}

    public record UpdateAction(
            @NotBlank
            String description,
            String revisionReasonLabel  // nullable — 이유 선택은 선택 항목
    ) {}
}