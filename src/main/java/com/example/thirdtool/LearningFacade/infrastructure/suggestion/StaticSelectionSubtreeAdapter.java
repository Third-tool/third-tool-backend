package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChapterSubtreeResponse;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.SelectionSubtreePort;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.SelectionSubtreeRequest;
import com.example.thirdtool.LearningFacade.application.service.RoleDetector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Static SelectionSubtree Adapter (Story-AS-E2-S2-8, 이슈 #17).
 *
 * <p>catalog의 selectionOutlines에서 (axisName · selectionName · chapter.title) 매칭 후
 * subtree ASCII 반환. 매칭 실패 시 suggestionsAvailable=false.
 */
@Component
@ConditionalOnProperty(
        name = "thirdtool.suggestion.provider",
        havingValue = "static",
        matchIfMissing = true
)
public class StaticSelectionSubtreeAdapter implements SelectionSubtreePort {

    private final SuggestionCatalogLoader catalogLoader;
    private final RoleDetector roleDetector;

    public StaticSelectionSubtreeAdapter(SuggestionCatalogLoader catalogLoader, RoleDetector roleDetector) {
        this.catalogLoader = catalogLoader;
        this.roleDetector = roleDetector;
    }

    @Override
    public ChapterSubtreeResponse suggest(SelectionSubtreeRequest request) {
        String role = roleDetector.detect(request.concepts());
        SuggestionCatalog catalog = catalogLoader.load(role);
        String providerContext = "static:" + (role == null ? "generic" : role);

        if (catalog.selectionOutlines() == null || catalog.selectionOutlines().isEmpty()) {
            return ChapterSubtreeResponse.unavailable(providerContext);
        }

        for (SuggestionCatalog.SelectionOutlineEntry entry : catalog.selectionOutlines()) {
            if (entry == null || entry.axisName() == null || entry.chapters() == null) continue;
            if (!entry.axisName().equalsIgnoreCase(request.axisName())) continue;
            if (entry.nameCandidate() != null
                    && !entry.nameCandidate().equalsIgnoreCase(request.selectionName())) {
                continue;
            }
            for (SuggestionCatalog.ChapterEntry chapter : entry.chapters()) {
                if (chapter == null || chapter.subtree() == null || chapter.subtree().isBlank()) continue;
                if (chapter.title() != null
                        && chapter.title().equalsIgnoreCase(request.chapter().title())) {
                    return new ChapterSubtreeResponse(chapter.subtree(), providerContext, true);
                }
            }
        }
        return ChapterSubtreeResponse.unavailable(providerContext);
    }
}
