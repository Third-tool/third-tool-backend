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

    // ─── Topic ────────────────────────────────────────────

    public record AddTopic(
            @NotBlank
            String name,
            String description
    ) {}

    public record UpdateTopic(
            @NotBlank
            String name,
            String description
    ) {}
}
