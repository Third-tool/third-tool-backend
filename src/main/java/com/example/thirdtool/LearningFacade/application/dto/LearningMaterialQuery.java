package com.example.thirdtool.LearningFacade.application.dto;

/**
 * LearningMaterial Query 입력 객체 묶음.
 *
 * <p>현재 LearningMaterial 측 Query는 {@link GetMaterials} 1종 (Facade 소속 자료 목록).
 * Command/Query 분리 원칙상 별도 record로 두며, 향후 필터·정렬 옵션 추가 시 본 record가
 * 자연스러운 확장 지점이 된다.
 */
public final class LearningMaterialQuery {

    private LearningMaterialQuery() {}

    public record GetMaterials(
            Long userId
    ) {}
}
