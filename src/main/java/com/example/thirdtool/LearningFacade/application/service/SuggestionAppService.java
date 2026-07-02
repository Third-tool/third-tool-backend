package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.application.dto.SuggestionCommand;
import com.example.thirdtool.LearningFacade.application.dto.SuggestionResult;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 6-Port Suggestion Application Service (Story-AS-E5-S5-3 부분, M3 최소 형태).
 *
 * <p>무상태 조율 서비스. 4개 신규 Port를 REST 계층에서 호출한다.
 * 입출력은 application-layer Command/Result records — presentation dto와 의존 분리.
 *
 * <p>@ConditionalOnBean: 4개 Port 구현체(Static Adapter 등) 없이는 활성화되지 않음.
 * LLM Adapter 구현 전(provider=llm 미배선 상태)엔 자동으로 로드 스킵.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ConditionalOnBean({
        ChaptersOutlinePort.class, ChapterSubtreePort.class,
        SelectionOutlinePort.class, SelectionSubtreePort.class
})
public class SuggestionAppService {

    private final ChaptersOutlinePort chaptersOutlinePort;
    private final ChapterSubtreePort chapterSubtreePort;
    private final SelectionOutlinePort selectionOutlinePort;
    private final SelectionSubtreePort selectionSubtreePort;

    public SuggestionResult.ChaptersOutline chaptersOutline(SuggestionCommand.ChaptersOutline cmd) {
        ChaptersOutlineResponse response = chaptersOutlinePort.suggest(new ChaptersOutlineRequest(
                cmd.concepts(), cmd.layerName(), cmd.axisName(), cmd.axisReason(),
                cmd.chapterCountHint(), cmd.freeformHint()));

        return new SuggestionResult.ChaptersOutline(
                response.chapters(), response.providerContext(), response.suggestionsAvailable());
    }

    public SuggestionResult.ChapterSubtree chapterSubtree(SuggestionCommand.ChapterSubtree cmd) {
        List<ChapterOutlineItem> siblings = cmd.siblingChapters() == null ? List.of()
                : cmd.siblingChapters();

        ChapterSubtreeResponse response = chapterSubtreePort.suggest(new ChapterSubtreeRequest(
                cmd.concepts(), cmd.layerName(), cmd.axisName(), cmd.chapter(), siblings));

        return new SuggestionResult.ChapterSubtree(
                response.bodyAsciiTree(), response.providerContext(), response.suggestionsAvailable());
    }

    public SuggestionResult.SelectionOutline selectionOutline(SuggestionCommand.SelectionOutline cmd) {
        SelectionOutlineResponse response = selectionOutlinePort.suggest(new SelectionOutlineRequest(
                cmd.concepts(), cmd.layerName(), cmd.axisName(),
                cmd.roadmapContent(), cmd.variantHint(), cmd.chapterCountHint()));

        return new SuggestionResult.SelectionOutline(
                response.nameCandidate(), response.chapters(),
                response.providerContext(), response.suggestionsAvailable());
    }

    public SuggestionResult.ChapterSubtree selectionSubtree(SuggestionCommand.SelectionSubtree cmd) {
        List<ChapterOutlineItem> siblings = cmd.selectionSiblings() == null ? List.of()
                : cmd.selectionSiblings();

        ChapterSubtreeResponse response = selectionSubtreePort.suggest(new SelectionSubtreeRequest(
                cmd.concepts(), cmd.layerName(), cmd.axisName(), cmd.chapter(),
                cmd.selectionName(), siblings));

        return new SuggestionResult.ChapterSubtree(
                response.bodyAsciiTree(), response.providerContext(), response.suggestionsAvailable());
    }
}
