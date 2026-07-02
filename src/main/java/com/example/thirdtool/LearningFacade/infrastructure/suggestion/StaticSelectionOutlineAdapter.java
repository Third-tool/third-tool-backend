package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.ChapterOutlineItem;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.SelectionOutlinePort;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.SelectionOutlineRequest;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.SelectionOutlineResponse;
import com.example.thirdtool.LearningFacade.application.service.RoleDetector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Static SelectionOutline Adapter (Story-AS-E2-S2-7, 이슈 #17).
 *
 * <p>catalog의 selectionOutlines에서 axisName 매칭 + (variantHint 있으면 우선 매칭) 후
 * nameCandidate + chapters(title/rationale) 반환.
 */
@Component
@ConditionalOnProperty(
        name = "thirdtool.suggestion.provider",
        havingValue = "static",
        matchIfMissing = true
)
public class StaticSelectionOutlineAdapter implements SelectionOutlinePort {

    private final SuggestionCatalogLoader catalogLoader;
    private final RoleDetector roleDetector;

    public StaticSelectionOutlineAdapter(SuggestionCatalogLoader catalogLoader, RoleDetector roleDetector) {
        this.catalogLoader = catalogLoader;
        this.roleDetector = roleDetector;
    }

    @Override
    public SelectionOutlineResponse suggest(SelectionOutlineRequest request) {
        String role = roleDetector.detect(request.concepts());
        SuggestionCatalog catalog = catalogLoader.load(role);
        String providerContext = "static:" + (role == null ? "generic" : role);

        if (catalog.selectionOutlines() == null || catalog.selectionOutlines().isEmpty()) {
            return SelectionOutlineResponse.unavailable(providerContext);
        }

        // axis + variantHint 매칭이 있으면 우선, 없으면 axis만 매칭한 첫 항목.
        SuggestionCatalog.SelectionOutlineEntry axisMatched = null;
        SuggestionCatalog.SelectionOutlineEntry hintMatched = null;

        for (SuggestionCatalog.SelectionOutlineEntry entry : catalog.selectionOutlines()) {
            if (entry == null || entry.axisName() == null) continue;
            if (!entry.axisName().equalsIgnoreCase(request.axisName())) continue;

            if (axisMatched == null) {
                axisMatched = entry;
            }
            if (hintMatched == null && request.variantHint() != null && entry.variantHint() != null
                    && entry.variantHint().toLowerCase().contains(request.variantHint().toLowerCase())) {
                hintMatched = entry;
                break;
            }
        }

        SuggestionCatalog.SelectionOutlineEntry chosen = hintMatched != null ? hintMatched : axisMatched;
        if (chosen == null) {
            return SelectionOutlineResponse.unavailable(providerContext);
        }

        List<ChapterOutlineItem> chapters = chosen.chapters() == null ? List.of()
                : chosen.chapters().stream()
                    .filter(c -> c != null && c.title() != null && !c.title().isBlank())
                    .map(c -> new ChapterOutlineItem(c.title(), c.rationale()))
                    .toList();

        int limit = request.chapterCountHint() != null && request.chapterCountHint() > 0
                ? Math.min(chapters.size(), request.chapterCountHint()) : chapters.size();
        // subList는 원본 view — 불변 복사로 감싼다.
        List<ChapterOutlineItem> limited = List.copyOf(chapters.subList(0, limit));

        return new SelectionOutlineResponse(chosen.nameCandidate(), limited, providerContext, true);
    }
}
