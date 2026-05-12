package com.example.thirdtool.LearningFacade.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaterialType 분류 (Story-003-4)")
class MaterialTypeTest {

    @ParameterizedTest(name = "{0} → static={1}, dynamic={2}")
    @CsvSource({
            "BOOK,            true,  false",
            "COURSE,          true,  false",
            "AI_CONVERSATION, false, true",
            "WEB_RESOURCE,    false, true"
    })
    @DisplayName("isStatic / isDynamic — 정적 자료(BOOK·COURSE) vs 동적 자료(AI_CONVERSATION·WEB_RESOURCE) 분기")
    void isStatic_isDynamic_정적_동적_분류(MaterialType type, boolean isStatic, boolean isDynamic) {
        assertThat(type.isStatic()).isEqualTo(isStatic);
        assertThat(type.isDynamic()).isEqualTo(isDynamic);
    }

    @ParameterizedTest
    @CsvSource({"BOOK", "COURSE", "AI_CONVERSATION", "WEB_RESOURCE"})
    @DisplayName("displayName / description 메타데이터 — 모든 enum 값이 비어있지 않은 값을 가진다")
    void displayName_description_비어있지않음(MaterialType type) {
        assertThat(type.getDisplayName()).isNotBlank();
        assertThat(type.getDescription()).isNotBlank();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("static + dynamic은 서로 배타적이며 4개 enum이 빠짐없이 둘 중 하나에 속한다")
    void static_dynamic_disjoint_total() {
        for (MaterialType type : MaterialType.values()) {
            assertThat(type.isStatic() ^ type.isDynamic())
                    .as("type %s must be exactly one of static/dynamic", type)
                    .isTrue();
        }
    }
}
