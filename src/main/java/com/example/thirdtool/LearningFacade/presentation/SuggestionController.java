package com.example.thirdtool.LearningFacade.presentation;

import com.example.thirdtool.LearningFacade.application.service.SuggestionAppService;
import com.example.thirdtool.LearningFacade.presentation.dto.SuggestionRequest;
import com.example.thirdtool.LearningFacade.presentation.dto.SuggestionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 6-Port Suggestion REST Controller (Story-AS-E5-S5-3 부분, 이슈 #17).
 *
 * <p>M3 하이라이트: `POST /api/v1/suggestions/chapters-outline` 첫 응답.
 * v1은 Static Adapter가 실제 응답 발행 (providerContext = "static:{role}").
 * LLM Adapter는 M6.
 */
@RestController
@RequestMapping("/api/v1/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionAppService suggestionAppService;

    // POST /api/v1/suggestions/chapters-outline
    @PostMapping("/chapters-outline")
    public SuggestionResponse.ChaptersOutlineResponseBody chaptersOutline(
            @Valid @RequestBody SuggestionRequest.ChaptersOutlineRequest request
    ) {
        return suggestionAppService.chaptersOutline(request);
    }

    // POST /api/v1/suggestions/chapter-subtree
    @PostMapping("/chapter-subtree")
    public SuggestionResponse.ChapterSubtreeResponseBody chapterSubtree(
            @Valid @RequestBody SuggestionRequest.ChapterSubtreeRequest request
    ) {
        return suggestionAppService.chapterSubtree(request);
    }

    // POST /api/v1/suggestions/selection-outline
    @PostMapping("/selection-outline")
    public SuggestionResponse.SelectionOutlineResponseBody selectionOutline(
            @Valid @RequestBody SuggestionRequest.SelectionOutlineRequest request
    ) {
        return suggestionAppService.selectionOutline(request);
    }

    // POST /api/v1/suggestions/selection-subtree
    @PostMapping("/selection-subtree")
    public SuggestionResponse.ChapterSubtreeResponseBody selectionSubtree(
            @Valid @RequestBody SuggestionRequest.SelectionSubtreeRequest request
    ) {
        return suggestionAppService.selectionSubtree(request);
    }
}
