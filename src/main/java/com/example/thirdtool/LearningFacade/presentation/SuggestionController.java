package com.example.thirdtool.LearningFacade.presentation;

import com.example.thirdtool.LearningFacade.application.dto.SuggestionCommand;
import com.example.thirdtool.LearningFacade.application.dto.SuggestionResult;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChapterOutlineItem;
import com.example.thirdtool.LearningFacade.application.service.SuggestionAppService;
import com.example.thirdtool.LearningFacade.presentation.dto.SuggestionRequest;
import com.example.thirdtool.LearningFacade.presentation.dto.SuggestionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 6-Port Suggestion REST Controller (Story-AS-E5-S5-3 부분, 이슈 #17).
 *
 * <p>M3 하이라이트: `POST /api/v1/suggestions/chapters-outline` 첫 응답.
 * v1은 Static Adapter가 응답 발행 (providerContext = "static:{role}"). LLM Adapter는 M6.
 *
 * <p>모든 엔드포인트는 성공/실패 모두 200 반환 + `suggestionsAvailable` 플래그로 응답 유효성 표현 (ADR010).
 *
 * <p>{@link SuggestionAppService}가 활성화된 경우에만 컨트롤러 로드 (Port 구현체 부재 시 스킵).
 */
@RestController
@RequestMapping("/api/v1/suggestions")
@RequiredArgsConstructor
@ConditionalOnBean(SuggestionAppService.class)
public class SuggestionController {

    private final SuggestionAppService suggestionAppService;

    @PostMapping("/chapters-outline")
    @ResponseStatus(HttpStatus.OK)
    public SuggestionResponse.ChaptersOutlineResponseBody chaptersOutline(
            @Valid @RequestBody SuggestionRequest.ChaptersOutlineRequest request
    ) {
        SuggestionResult.ChaptersOutline result = suggestionAppService.chaptersOutline(
                new SuggestionCommand.ChaptersOutline(
                        request.concepts(), request.layerName(), request.axisName(),
                        request.axisReason(), request.chapterCountHint(), request.freeformHint()));
        return new SuggestionResponse.ChaptersOutlineResponseBody(
                result.chapters().stream()
                        .map(SuggestionResponse.ChapterOutlineItemResponse::of)
                        .toList(),
                result.providerContext(),
                result.suggestionsAvailable());
    }

    @PostMapping("/chapter-subtree")
    @ResponseStatus(HttpStatus.OK)
    public SuggestionResponse.ChapterSubtreeResponseBody chapterSubtree(
            @Valid @RequestBody SuggestionRequest.ChapterSubtreeRequest request
    ) {
        SuggestionResult.ChapterSubtree result = suggestionAppService.chapterSubtree(
                new SuggestionCommand.ChapterSubtree(
                        request.concepts(), request.layerName(), request.axisName(),
                        toChapterOutlineItem(request.chapter()),
                        toChapterOutlineItems(request.siblingChapters())));
        return new SuggestionResponse.ChapterSubtreeResponseBody(
                result.bodyAsciiTree(), result.providerContext(), result.suggestionsAvailable());
    }

    @PostMapping("/selection-outline")
    @ResponseStatus(HttpStatus.OK)
    public SuggestionResponse.SelectionOutlineResponseBody selectionOutline(
            @Valid @RequestBody SuggestionRequest.SelectionOutlineRequest request
    ) {
        SuggestionResult.SelectionOutline result = suggestionAppService.selectionOutline(
                new SuggestionCommand.SelectionOutline(
                        request.concepts(), request.layerName(), request.axisName(),
                        request.roadmapContent(), request.variantHint(), request.chapterCountHint()));
        return new SuggestionResponse.SelectionOutlineResponseBody(
                result.nameCandidate(),
                result.chapters().stream()
                        .map(SuggestionResponse.ChapterOutlineItemResponse::of)
                        .toList(),
                result.providerContext(),
                result.suggestionsAvailable());
    }

    @PostMapping("/selection-subtree")
    @ResponseStatus(HttpStatus.OK)
    public SuggestionResponse.ChapterSubtreeResponseBody selectionSubtree(
            @Valid @RequestBody SuggestionRequest.SelectionSubtreeRequest request
    ) {
        SuggestionResult.ChapterSubtree result = suggestionAppService.selectionSubtree(
                new SuggestionCommand.SelectionSubtree(
                        request.concepts(), request.layerName(), request.axisName(),
                        toChapterOutlineItem(request.chapter()),
                        request.selectionName(),
                        toChapterOutlineItems(request.selectionSiblings())));
        return new SuggestionResponse.ChapterSubtreeResponseBody(
                result.bodyAsciiTree(), result.providerContext(), result.suggestionsAvailable());
    }

    private ChapterOutlineItem toChapterOutlineItem(SuggestionRequest.ChapterOutlineItemDto dto) {
        return new ChapterOutlineItem(dto.title(), dto.rationale());
    }

    private List<ChapterOutlineItem> toChapterOutlineItems(List<SuggestionRequest.ChapterOutlineItemDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream()
                .filter(d -> d != null && d.title() != null && !d.title().isBlank())
                .map(this::toChapterOutlineItem)
                .toList();
    }
}
