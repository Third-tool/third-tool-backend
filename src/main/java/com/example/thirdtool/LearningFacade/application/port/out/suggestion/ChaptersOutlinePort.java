package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

/**
 * AI 챕터 outline 생성 outbound Port (Story-AS-E1-S1-5, 이슈 #17).
 *
 * <p>이전 {@code RoadmapSuggestionPort} (이슈 #9, axis 전체 트리 통짜)는 SUPERSEDED.
 * 대신 outline(챕터 리스트만) → subtree(챕터별 병렬) 2단계 flow로 재편.
 *
 * <p>구현체는 Static(카탈로그 조회) / LLM(Vertex AI) 등으로 교체 가능.
 * 실패 시 {@link ChaptersOutlineResponse#unavailable}로 응답 (ADR010 fallback).
 */
public interface ChaptersOutlinePort {

    ChaptersOutlineResponse suggest(ChaptersOutlineRequest request);
}
