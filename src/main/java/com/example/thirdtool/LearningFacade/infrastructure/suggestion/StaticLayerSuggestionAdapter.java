package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.example.thirdtool.LearningFacade.application.port.out.suggestion.LayerSuggestion;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.LayerSuggestionContext;
import com.example.thirdtool.LearningFacade.application.port.out.suggestion.LayerSuggestionPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Static (LLM 없이 카탈로그 기반) Layer 제안 Adapter (Story-AS-E2-S1).
 *
 * <p>{@link LayerSuggestionContext#role()}를 카탈로그 키로 조회 → 매칭 카탈로그의 layers 절 반환.
 * role이 null이면 "generic" 카탈로그로 폴백 ({@link SuggestionCatalogLoader#load}가 처리).
 * {@code existingLayerNames}에 포함된 이름은 후보에서 제외.
 *
 * <p>기본 활성화 조건: {@code thirdtool.suggestion.provider=static} (또는 미설정 시 기본).
 * LLM Adapter 도입 시 property로 교체.
 */
@Component
@ConditionalOnProperty(
        name = "thirdtool.suggestion.provider",
        havingValue = "static",
        matchIfMissing = true
)
public class StaticLayerSuggestionAdapter implements LayerSuggestionPort {

    private final SuggestionCatalogLoader catalogLoader;

    public StaticLayerSuggestionAdapter(SuggestionCatalogLoader catalogLoader) {
        this.catalogLoader = catalogLoader;
    }

    @Override
    public List<LayerSuggestion> suggest(LayerSuggestionContext context,
                                         List<String> existingLayerNames,
                                         int limit) {
        if (limit <= 0) {
            return List.of();
        }
        SuggestionCatalog catalog = catalogLoader.load(context.role());
        List<SuggestionCatalog.LayerEntry> layerEntries = catalog.layers();
        if (layerEntries == null || layerEntries.isEmpty()) {
            return List.of();
        }

        // 기존 Layer 이름 정규화 (trim, null 제외) — 도메인 컨벤션 §1.1.
        Set<String> excluded = new HashSet<>();
        if (existingLayerNames != null) {
            for (String name : existingLayerNames) {
                if (name != null) {
                    excluded.add(name.trim());
                }
            }
        }

        List<LayerSuggestion> filtered = new ArrayList<>();
        for (SuggestionCatalog.LayerEntry entry : layerEntries) {
            if (entry == null || entry.name() == null || entry.name().isBlank()) {
                continue;
            }
            String name = entry.name().trim();
            if (excluded.contains(name)) {
                continue;
            }
            filtered.add(new LayerSuggestion(name, entry.rationale()));
            if (filtered.size() >= limit) {
                break;
            }
        }
        return filtered;
    }
}
