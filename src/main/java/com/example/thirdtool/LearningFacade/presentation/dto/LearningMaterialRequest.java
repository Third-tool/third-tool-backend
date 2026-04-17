package com.example.thirdtool.LearningFacade.presentation.dto;

import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class LearningMaterialRequest {

    public record CreateMaterial(
            @NotBlank
            String name,

            @NotNull
            MaterialType materialType,

            String url,                     // nullable — v1 선택 항목
            List<Long> linkedActionIds      // nullable — 미입력 시 빈 목록
    ) {}

    public record UpdateMaterialName(
            @NotBlank
            String name
    ) {}

    public record UpdateProficiency(
            @NotNull
            ProficiencyLevel proficiencyLevel
    ) {}

    public record LinkAction(
            @NotNull
            Long actionId
    ) {}
}