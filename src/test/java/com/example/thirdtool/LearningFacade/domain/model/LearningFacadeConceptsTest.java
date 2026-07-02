package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Story-LT-E1-S2 · concepts[] 컬렉션 API 테스트.
 * 해피 / 엣지 / 예외 세트 커버.
 */
@DisplayName("LearningFacade · concepts[] 컬렉션 API")
class LearningFacadeConceptsTest {

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("tester", "encoded-pw", "닉", "t@t.com");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    private LearningFacade createFacade() {
        return LearningFacade.create(user, "백엔드");
    }

    private static LearningFacadeConcept addConceptWithId(LearningFacade facade, String value, Long id) {
        LearningFacadeConcept c = facade.addConcept(value);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    // ─── 생성 (create 팩토리와 concepts 동기화) ─────────────

    @Nested
    @DisplayName("create — 단수 concept은 concepts[0]으로 동기화")
    class CreateSync {

        @Test
        @DisplayName("create_valid_concepts에도_첫_항목으로_추가된다")
        void create_valid_concepts에도_첫_항목으로_추가된다() {
            LearningFacade facade = LearningFacade.create(user, "백엔드");

            assertThat(facade.getConcept()).isEqualTo("백엔드");
            assertThat(facade.getConcepts()).hasSize(1);
            assertThat(facade.getConcepts().get(0).getValue()).isEqualTo("백엔드");
            assertThat(facade.getConcepts().get(0).getDisplayOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("create_concept_공백_포함이면_trim된_값으로_concepts에도_저장")
        void create_concept_공백_포함이면_trim된_값으로_concepts에도_저장() {
            LearningFacade facade = LearningFacade.create(user, "  백엔드  ");
            assertThat(facade.getConceptValues()).containsExactly("백엔드");
        }
    }

    // ─── addConcept — 해피 ──────────────────────────────────

    @Nested
    @DisplayName("addConcept — 해피")
    class AddHappy {

        @Test
        @DisplayName("addConcept_첫번째_이후_추가_displayOrder는_마지막플러스1")
        void addConcept_첫번째_이후_추가_displayOrder는_마지막플러스1() {
            LearningFacade facade = createFacade();
            LearningFacadeConcept second = facade.addConcept("시스템설계");

            assertThat(facade.getConcepts()).hasSize(2);
            assertThat(second.getDisplayOrder()).isEqualTo(2);
            assertThat(facade.getConceptValues()).containsExactly("백엔드", "시스템설계");
        }

        @Test
        @DisplayName("addConcept_공백_포함_값은_trim되어_저장")
        void addConcept_공백_포함_값은_trim되어_저장() {
            LearningFacade facade = createFacade();
            facade.addConcept("  분산시스템  ");
            assertThat(facade.getConceptValues()).containsExactly("백엔드", "분산시스템");
        }

        @Test
        @DisplayName("addConcept_5개까지는_정상_허용")
        void addConcept_5개까지는_정상_허용() {
            LearningFacade facade = createFacade(); // 1개
            facade.addConcept("B");
            facade.addConcept("C");
            facade.addConcept("D");
            facade.addConcept("E");
            assertThat(facade.getConcepts()).hasSize(5);
        }
    }

    // ─── addConcept — 엣지·예외 ─────────────────────────────

    @Nested
    @DisplayName("addConcept — 예외")
    class AddException {

        @Test
        @DisplayName("addConcept_blank_예외")
        void addConcept_blank_예외() {
            LearningFacade facade = createFacade();
            assertThatThrownBy(() -> facade.addConcept("   "))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPT_BLANK);
        }

        @Test
        @DisplayName("addConcept_null_예외")
        void addConcept_null_예외() {
            LearningFacade facade = createFacade();
            assertThatThrownBy(() -> facade.addConcept(null))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPT_BLANK);
        }

        @Test
        @DisplayName("addConcept_동일값_trim후_중복_예외")
        void addConcept_동일값_trim후_중복_예외() {
            LearningFacade facade = createFacade(); // "백엔드"
            assertThatThrownBy(() -> facade.addConcept("  백엔드  "))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPT_DUPLICATE);
        }

        @Test
        @DisplayName("addConcept_6번째_추가시_SIZE_INVALID_예외")
        void addConcept_6번째_추가시_SIZE_INVALID_예외() {
            LearningFacade facade = createFacade(); // 1개
            facade.addConcept("B");
            facade.addConcept("C");
            facade.addConcept("D");
            facade.addConcept("E"); // 총 5개

            assertThatThrownBy(() -> facade.addConcept("F"))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPTS_SIZE_INVALID);
        }

        @Test
        @DisplayName("addConcept_101자_TOO_LONG_예외")
        void addConcept_101자_TOO_LONG_예외() {
            LearningFacade facade = createFacade();
            String longValue = "a".repeat(101);
            assertThatThrownBy(() -> facade.addConcept(longValue))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPT_TOO_LONG);
        }
    }

    // ─── getConcepts unmodifiable ──────────────────────────

    @Nested
    @DisplayName("getConcepts — 캡슐화")
    class Encapsulation {

        @Test
        @DisplayName("getConcepts는_unmodifiable_리스트를_반환_add시_예외")
        void getConcepts는_unmodifiable_리스트를_반환_add시_예외() {
            LearningFacade facade = createFacade();
            List<LearningFacadeConcept> view = facade.getConcepts();

            assertThatThrownBy(() -> view.add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ─── removeConcept ─────────────────────────────────────

    @Nested
    @DisplayName("removeConcept")
    class Remove {

        @Test
        @DisplayName("removeConcept_정상_displayOrder_재부여")
        void removeConcept_정상_displayOrder_재부여() {
            LearningFacade facade = createFacade();
            LearningFacadeConcept c2 = addConceptWithId(facade, "B", 20L);
            LearningFacadeConcept c3 = addConceptWithId(facade, "C", 30L);
            // c1은 create() 시점에 생성되어 id 없음 — 첫 항목 id 부여 필요
            ReflectionTestUtils.setField(facade.getConcepts().get(0), "id", 10L);

            facade.removeConcept(20L); // c2 제거

            assertThat(facade.getConcepts()).hasSize(2);
            assertThat(facade.getConceptValues()).containsExactly("백엔드", "C");
            assertThat(facade.getConcepts().get(0).getDisplayOrder()).isEqualTo(1);
            assertThat(facade.getConcepts().get(1).getDisplayOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("removeConcept_마지막1건이면_예외")
        void removeConcept_마지막1건이면_예외() {
            LearningFacade facade = createFacade();
            ReflectionTestUtils.setField(facade.getConcepts().get(0), "id", 10L);

            assertThatThrownBy(() -> facade.removeConcept(10L))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPTS_SIZE_INVALID);
        }

        @Test
        @DisplayName("removeConcept_존재하지_않는_id_예외")
        void removeConcept_존재하지_않는_id_예외() {
            LearningFacade facade = createFacade();
            addConceptWithId(facade, "B", 20L);
            ReflectionTestUtils.setField(facade.getConcepts().get(0), "id", 10L);

            assertThatThrownBy(() -> facade.removeConcept(999L))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        }
    }

    // ─── reorderConcepts ───────────────────────────────────

    @Nested
    @DisplayName("reorderConcepts")
    class Reorder {

        @Test
        @DisplayName("reorderConcepts_정상_displayOrder_재부여")
        void reorderConcepts_정상_displayOrder_재부여() {
            LearningFacade facade = createFacade();
            ReflectionTestUtils.setField(facade.getConcepts().get(0), "id", 10L);
            addConceptWithId(facade, "B", 20L);
            addConceptWithId(facade, "C", 30L);

            facade.reorderConcepts(List.of(30L, 10L, 20L));

            assertThat(facade.getConceptValues()).containsExactly("C", "백엔드", "B");
        }

        @Test
        @DisplayName("reorderConcepts_id집합_불일치_예외")
        void reorderConcepts_id집합_불일치_예외() {
            LearningFacade facade = createFacade();
            ReflectionTestUtils.setField(facade.getConcepts().get(0), "id", 10L);
            addConceptWithId(facade, "B", 20L);

            assertThatThrownBy(() -> facade.reorderConcepts(List.of(10L, 99L)))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPTS_REORDER_MISMATCH);
        }

        @Test
        @DisplayName("reorderConcepts_길이_불일치_예외")
        void reorderConcepts_길이_불일치_예외() {
            LearningFacade facade = createFacade();
            ReflectionTestUtils.setField(facade.getConcepts().get(0), "id", 10L);
            addConceptWithId(facade, "B", 20L);

            assertThatThrownBy(() -> facade.reorderConcepts(List.of(10L)))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPTS_REORDER_MISMATCH);
        }
    }
}
