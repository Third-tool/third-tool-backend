package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChapterOutlineItem;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChaptersOutlinePort;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChaptersOutlineRequest;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChaptersOutlineResponse;
import com.example.thirdtool.LearningFacade.application.service.RoleDetector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Static ChaptersOutline Adapter (Story-AS-E2-S2-5, 이슈 #17).
 *
 * <p>concepts에서 role 감지 → role-keyed catalog 조회 → axisName 매칭 챕터의 title/rationale만 반환.
 * chapterCountHint 있으면 상위 N개만.
 * catalog 매칭 실패 시 suggestionsAvailable=false.
 */
@Component
@ConditionalOnProperty(
        name = "thirdtool.suggestion.provider",
        havingValue = "static",
        matchIfMissing = true
)
public class StaticChaptersOutlineAdapter implements ChaptersOutlinePort {

    private static final int DEFAULT_CHAPTER_COUNT = 5;

    private final SuggestionCatalogLoader catalogLoader;
    private final RoleDetector roleDetector;

    public StaticChaptersOutlineAdapter(SuggestionCatalogLoader catalogLoader, RoleDetector roleDetector) {
        this.catalogLoader = catalogLoader;
        this.roleDetector = roleDetector;
    }

    @Override
    public ChaptersOutlineResponse suggest(ChaptersOutlineRequest request) {
        String role = roleDetector.detect(request.concepts());
        SuggestionCatalog catalog = catalogLoader.load(role);
        String providerContext = "static:" + (role == null ? "generic" : role);

        List<SuggestionCatalog.ChapterEntry> chapters = catalog.chapters();
        if (chapters == null || chapters.isEmpty()) {
            return ChaptersOutlineResponse.unavailable(providerContext);
        }

        int limit = request.chapterCountHint() != null && request.chapterCountHint() > 0
                ? request.chapterCountHint() : DEFAULT_CHAPTER_COUNT;

        List<ChapterOutlineItem> filtered = new ArrayList<>();
        for (SuggestionCatalog.ChapterEntry entry : chapters) {
            if (entry == null || entry.title() == null || entry.title().isBlank()) {
                continue;
            }
            if (entry.axisName() != null && !entry.axisName().equalsIgnoreCase(request.axisName())) {
                continue;
            }
            filtered.add(new ChapterOutlineItem(entry.title(), entry.rationale()));
            if (filtered.size() >= limit) {
                break;
            }
        }

        if (filtered.isEmpty()) {
            return ChaptersOutlineResponse.unavailable(providerContext);
        }
        return new ChaptersOutlineResponse(filtered, providerContext, true);
    }
}
