package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.*;
import com.example.thirdtool.LearningFacade.presentation.dto.SuggestionRequest;
import com.example.thirdtool.LearningFacade.presentation.dto.SuggestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 6-Port Suggestion Application Service (Story-AS-E5-S5-3 부분, M3 최소 형태).
 *
 * <p>무상태 조율 서비스. 4개 신규 Port(Chapters/Chapter/Selection outline/subtree)를 REST 계층에서
 * 호출한다. Layer/Axis Port는 SuggestionConceptContext 요구가 REST DTO에 반영되지 않아 별도 브랜치.
 *
 * <p>도메인 저장 없음 (Read-only transaction).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SuggestionAppService {

    private final ChaptersOutlinePort chaptersOutlinePort;
    private final ChapterSubtreePort chapterSubtreePort;
    private final SelectionOutlinePort selectionOutlinePort;
    private final SelectionSubtreePort selectionSubtreePort;

    public SuggestionResponse.ChaptersOutlineResponseBody chaptersOutline(
            SuggestionRequest.ChaptersOutlineRequest req) {
        ChaptersOutlineResponse response = chaptersOutlinePort.suggest(new ChaptersOutlineRequest(
                req.concepts(), req.layerName(), req.axisName(), req.axisReason(),
                req.chapterCountHint(), req.freeformHint()));

        return new SuggestionResponse.ChaptersOutlineResponseBody(
                response.chapters().stream()
                        .map(SuggestionResponse.ChapterOutlineItemResponse::of)
                        .toList(),
                response.providerContext(),
                response.suggestionsAvailable());
    }

    public SuggestionResponse.ChapterSubtreeResponseBody chapterSubtree(
            SuggestionRequest.ChapterSubtreeRequest req) {
        ChapterOutlineItem chapter = toChapterOutlineItem(req.chapter());
        List<ChapterOutlineItem> siblings = req.siblingChapters() == null ? List.of()
                : req.siblingChapters().stream().map(this::toChapterOutlineItem).toList();

        ChapterSubtreeResponse response = chapterSubtreePort.suggest(new ChapterSubtreeRequest(
                req.concepts(), req.layerName(), req.axisName(), chapter, siblings));

        return new SuggestionResponse.ChapterSubtreeResponseBody(
                response.bodyAsciiTree(),
                response.providerContext(),
                response.suggestionsAvailable());
    }

    public SuggestionResponse.SelectionOutlineResponseBody selectionOutline(
            SuggestionRequest.SelectionOutlineRequest req) {
        SelectionOutlineResponse response = selectionOutlinePort.suggest(new SelectionOutlineRequest(
                req.concepts(), req.layerName(), req.axisName(),
                req.roadmapContent(), req.variantHint(), req.chapterCountHint()));

        return new SuggestionResponse.SelectionOutlineResponseBody(
                response.nameCandidate(),
                response.chapters().stream()
                        .map(SuggestionResponse.ChapterOutlineItemResponse::of)
                        .toList(),
                response.providerContext(),
                response.suggestionsAvailable());
    }

    public SuggestionResponse.ChapterSubtreeResponseBody selectionSubtree(
            SuggestionRequest.SelectionSubtreeRequest req) {
        ChapterOutlineItem chapter = toChapterOutlineItem(req.chapter());
        List<ChapterOutlineItem> siblings = req.selectionSiblings() == null ? List.of()
                : req.selectionSiblings().stream().map(this::toChapterOutlineItem).toList();

        ChapterSubtreeResponse response = selectionSubtreePort.suggest(new SelectionSubtreeRequest(
                req.concepts(), req.layerName(), req.axisName(), chapter,
                req.selectionName(), siblings));

        return new SuggestionResponse.ChapterSubtreeResponseBody(
                response.bodyAsciiTree(),
                response.providerContext(),
                response.suggestionsAvailable());
    }

    private ChapterOutlineItem toChapterOutlineItem(SuggestionRequest.ChapterOutlineItemDto dto) {
        return new ChapterOutlineItem(dto.title(), dto.rationale());
    }
}
