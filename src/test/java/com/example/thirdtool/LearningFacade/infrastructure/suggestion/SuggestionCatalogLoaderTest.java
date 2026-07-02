package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story-AS-E3-S2 · classpath 카탈로그 로더 검증.
 */
@DisplayName("SuggestionCatalogLoader")
class SuggestionCatalogLoaderTest {

    private SuggestionCatalogLoader loader;

    @BeforeEach
    void setUp() {
        loader = new SuggestionCatalogLoader(new ObjectMapper());
    }

    @Test
    @DisplayName("load_backend_developer_layers_5건_반환")
    void load_backend_developer_layers_5건_반환() {
        SuggestionCatalog catalog = loader.load("backend-developer");

        assertThat(catalog.role()).isEqualTo("backend-developer");
        assertThat(catalog.layers()).hasSize(5);
        assertThat(catalog.layers().get(0).name()).isEqualTo("웹 API");
    }

    @Test
    @DisplayName("load_존재하지_않는_role_generic_fallback")
    void load_존재하지_않는_role_generic_fallback() {
        SuggestionCatalog catalog = loader.load("nonexistent-role");
        assertThat(catalog.role()).isEqualTo("generic");
        assertThat(catalog.layers()).isNotEmpty();
    }

    @Test
    @DisplayName("load_null_role_generic_fallback")
    void load_null_role_generic_fallback() {
        SuggestionCatalog catalog = loader.load(null);
        assertThat(catalog.role()).isEqualTo("generic");
    }

    @Test
    @DisplayName("load_blank_role_generic_fallback")
    void load_blank_role_generic_fallback() {
        SuggestionCatalog catalog = loader.load("  ");
        assertThat(catalog.role()).isEqualTo("generic");
    }

    @Test
    @DisplayName("load_동일_role_재요청시_캐시_반환_동일_인스턴스")
    void load_동일_role_재요청시_캐시_반환_동일_인스턴스() {
        SuggestionCatalog first = loader.load("backend-developer");
        SuggestionCatalog second = loader.load("backend-developer");
        assertThat(first).isSameAs(second);
    }
}
