package com.example.thirdtool.LearningFacade.presentation.dto;

import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaterialBreakdown VO 비중 집계 (Story-003-4 / Story 4-3)")
class MaterialBreakdownTest {

    @Test
    @DisplayName("from_빈리스트_모든_타입_0_total_0")
    void from_empty_all_zeros() {
        MaterialBreakdown bd = MaterialBreakdown.from(List.of());

        assertThat(bd.totalCount()).isZero();
        assertThat(bd.staticCount()).isZero();
        assertThat(bd.dynamicCount()).isZero();
        assertThat(bd.byType())
                .containsEntry(MaterialType.BOOK, 0L)
                .containsEntry(MaterialType.COURSE, 0L)
                .containsEntry(MaterialType.AI_CONVERSATION, 0L)
                .containsEntry(MaterialType.WEB_RESOURCE, 0L);
    }

    @Test
    @DisplayName("from_4종_혼합_byType_정확_static_dynamic_total_정합")
    void from_mixed_counts_correct() {
        MaterialBreakdown bd = MaterialBreakdown.from(List.of(
                MaterialType.BOOK,             // static
                MaterialType.COURSE,           // static
                MaterialType.AI_CONVERSATION,  // dynamic
                MaterialType.AI_CONVERSATION,  // dynamic
                MaterialType.WEB_RESOURCE,     // dynamic
                MaterialType.WEB_RESOURCE,     // dynamic
                MaterialType.WEB_RESOURCE      // dynamic
        ));

        assertThat(bd.byType())
                .containsEntry(MaterialType.BOOK, 1L)
                .containsEntry(MaterialType.COURSE, 1L)
                .containsEntry(MaterialType.AI_CONVERSATION, 2L)
                .containsEntry(MaterialType.WEB_RESOURCE, 3L);
        assertThat(bd.staticCount()).isEqualTo(2L);   // BOOK + COURSE
        assertThat(bd.dynamicCount()).isEqualTo(5L);  // AI(2) + WEB(3)
        assertThat(bd.totalCount()).isEqualTo(7L);    // static + dynamic
    }

    @Test
    @DisplayName("from_단일타입만_나머지_3종은_0_키_누락_안함 — FE 회색 처리 신호")
    void from_single_type_other_keys_present_with_zero() {
        MaterialBreakdown bd = MaterialBreakdown.from(List.of(
                MaterialType.BOOK, MaterialType.BOOK, MaterialType.BOOK));

        // 핵심: 0인 키도 누락하지 않음 (api §3 응답 필드 설명)
        assertThat(bd.byType()).hasSize(4);
        assertThat(bd.byType().get(MaterialType.BOOK)).isEqualTo(3L);
        assertThat(bd.byType().get(MaterialType.COURSE)).isZero();
        assertThat(bd.byType().get(MaterialType.AI_CONVERSATION)).isZero();
        assertThat(bd.byType().get(MaterialType.WEB_RESOURCE)).isZero();
        assertThat(bd.staticCount()).isEqualTo(3L);
        assertThat(bd.dynamicCount()).isZero();
        assertThat(bd.totalCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("from_정적만_dynamic_0 / 동적만_static_0 분류 정확")
    void from_static_only_vs_dynamic_only_classification() {
        MaterialBreakdown staticOnly = MaterialBreakdown.from(List.of(
                MaterialType.BOOK, MaterialType.COURSE));
        assertThat(staticOnly.staticCount()).isEqualTo(2L);
        assertThat(staticOnly.dynamicCount()).isZero();

        MaterialBreakdown dynamicOnly = MaterialBreakdown.from(List.of(
                MaterialType.AI_CONVERSATION, MaterialType.WEB_RESOURCE));
        assertThat(dynamicOnly.staticCount()).isZero();
        assertThat(dynamicOnly.dynamicCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("empty 팩토리는 from(빈리스트)와 동등")
    void empty_factory_equals_from_empty_list() {
        MaterialBreakdown viaEmpty = MaterialBreakdown.empty();
        MaterialBreakdown viaFrom = MaterialBreakdown.from(List.of());

        assertThat(viaEmpty).isEqualTo(viaFrom);
    }
}
