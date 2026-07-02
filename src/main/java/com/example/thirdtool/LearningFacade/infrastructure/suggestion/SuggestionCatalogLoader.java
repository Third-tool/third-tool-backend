package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * classpath에서 role별 카탈로그 JSON을 로드해 캐시한다 (Story-AS-E3-S2).
 *
 * <p>경로 규약: {@code ai/catalog/{role}.json}. role 파일이 없으면 {@code generic.json}으로 폴백.
 * generic 파일도 없으면 빈 카탈로그 반환 (Adapter가 빈 응답으로 변환).
 *
 * <p>캐시는 프로세스 lifetime 동안 유지 (JVM restart 시 재로드).
 */
@Component
public class SuggestionCatalogLoader {

    private static final String CATALOG_BASE_PATH = "ai/catalog/";
    private static final String CATALOG_EXTENSION = ".json";

    private final ObjectMapper objectMapper;
    private final Map<String, SuggestionCatalog> cache = new ConcurrentHashMap<>();

    public SuggestionCatalogLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * role에 해당하는 카탈로그를 반환. 없으면 generic로 폴백. generic도 없으면 빈 카탈로그.
     * 결과는 캐시된다 (동일 role 재요청 시 파일 재읽기 없음).
     */
    public SuggestionCatalog load(String role) {
        String effectiveRole = (role == null || role.isBlank()) ? "generic" : role;
        return cache.computeIfAbsent(effectiveRole, this::doLoad);
    }

    private SuggestionCatalog doLoad(String role) {
        SuggestionCatalog catalog = tryReadCatalog(role);
        if (catalog != null) {
            return catalog;
        }
        if (!"generic".equals(role)) {
            SuggestionCatalog fallback = tryReadCatalog("generic");
            if (fallback != null) {
                return fallback;
            }
        }
        return emptyCatalog(role);
    }

    private SuggestionCatalog tryReadCatalog(String role) {
        String path = CATALOG_BASE_PATH + role + CATALOG_EXTENSION;
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return null;
        }
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, SuggestionCatalog.class);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "카탈로그 JSON 파싱 실패: " + path, e);
        }
    }

    private static SuggestionCatalog emptyCatalog(String role) {
        return new SuggestionCatalog(role, List.of(), Map.of(), Map.of(), Map.of());
    }
}
