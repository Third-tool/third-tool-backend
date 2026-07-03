package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChapterSubtreePort;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChapterSubtreeRequest;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChapterSubtreeResponse;
import com.example.thirdtool.LearningFacade.application.service.RoleDetector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Static ChapterSubtree Adapter (Story-AS-E2-S2-6, 이슈 #17).
 *
 * <p>catalog의 chapters 중 axisName + title 매칭 항목의 subtree를 반환.
 * 매칭 실패 시 suggestionsAvailable=false.
 */
@Component
@ConditionalOnProperty(
        name = "thirdtool.suggestion.provider",
        havingValue = "static",
        matchIfMissing = true
)
public class StaticChapterSubtreeAdapter implements ChapterSubtreePort {

    private final SuggestionCatalogLoader catalogLoader;
    private final RoleDetector roleDetector;

    public StaticChapterSubtreeAdapter(SuggestionCatalogLoader catalogLoader, RoleDetector roleDetector) {
        this.catalogLoader = catalogLoader;
        this.roleDetector = roleDetector;
    }

    @Override
    public ChapterSubtreeResponse suggest(ChapterSubtreeRequest request) {
        String role = roleDetector.detect(request.concepts());
        SuggestionCatalog catalog = catalogLoader.load(role);
        String providerContext = "static:" + (role == null ? "generic" : role);

        if (catalog.chapters() == null || catalog.chapters().isEmpty()) {
            return ChapterSubtreeResponse.unavailable(providerContext);
        }

        for (SuggestionCatalog.ChapterEntry entry : catalog.chapters()) {
            if (entry == null || entry.subtree() == null || entry.subtree().isBlank()) {
                continue;
            }
            if (entry.axisName() != null && !entry.axisName().equalsIgnoreCase(request.axisName())) {
                continue;
            }
            if (entry.title() != null && entry.title().equalsIgnoreCase(request.chapter().title())) {
                return new ChapterSubtreeResponse(entry.subtree(), providerContext, true);
            }
        }
        return ChapterSubtreeResponse.unavailable(providerContext);
    }
}
