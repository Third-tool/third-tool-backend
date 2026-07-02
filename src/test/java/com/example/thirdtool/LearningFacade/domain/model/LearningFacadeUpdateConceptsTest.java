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
 * Story-LT-E1-S3 · {@link LearningFacade#updateConcepts(List)} 다건 통째 교체 테스트.
 */
@DisplayName("LearningFacade · updateConcepts")
class LearningFacadeUpdateConceptsTest {

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("tester", "encoded-pw", "닉", "t@t.com");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    private LearningFacade createFacade() {
        return LearningFacade.create(user, "백엔드"); // concepts=["백엔드"]
    }

    // ─── 해피 ──────────────────────────────────────────────

    @Nested
    @DisplayName("해피")
    class Happy {

        @Test
        @DisplayName("updateConcepts_새리스트로_통째_교체_added_removed_계산")
        void updateConcepts_새리스트로_통째_교체_added_removed_계산() {
            LearningFacade facade = createFacade();
            facade.addConcept("데이터모델링");
            // 현재: [백엔드, 데이터모델링]

            ConceptsChangeRecord rec = facade.updateConcepts(List.of("백엔드", "시스템설계", "분산시스템"));

            assertThat(facade.getConceptValues()).containsExactly("백엔드", "시스템설계", "분산시스템");
            assertThat(rec.getPrevious()).containsExactly("백엔드", "데이터모델링");
            assertThat(rec.getCurrent()).containsExactly("백엔드", "시스템설계", "분산시스템");
            assertThat(rec.getAdded()).containsExactly("시스템설계", "분산시스템");
            assertThat(rec.getRemoved()).containsExactly("데이터모델링");
            assertThat(rec.getKept()).containsExactly("백엔드");
            assertThat(rec.isChanged()).isTrue();
        }

        @Test
        @DisplayName("updateConcepts_동일리스트_isChanged_false")
        void updateConcepts_동일리스트_isChanged_false() {
            LearningFacade facade = createFacade();

            ConceptsChangeRecord rec = facade.updateConcepts(List.of("백엔드"));

            assertThat(rec.isChanged()).isFalse();
            assertThat(rec.getAdded()).isEmpty();
            assertThat(rec.getRemoved()).isEmpty();
        }

        @Test
        @DisplayName("updateConcepts_순서만_변경도_isChanged_true")
        void updateConcepts_순서만_변경도_isChanged_true() {
            LearningFacade facade = createFacade();
            facade.addConcept("B");

            ConceptsChangeRecord rec = facade.updateConcepts(List.of("B", "백엔드"));

            assertThat(rec.isChanged()).isTrue();
            assertThat(rec.getAdded()).isEmpty();
            assertThat(rec.getRemoved()).isEmpty();
        }

        @Test
        @DisplayName("updateConcepts_displayOrder_1부터_재부여")
        void updateConcepts_displayOrder_1부터_재부여() {
            LearningFacade facade = createFacade();

            facade.updateConcepts(List.of("A", "B", "C"));

            assertThat(facade.getConcepts()).extracting(LearningFacadeConcept::getDisplayOrder)
                                            .containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("updateConcepts_각_값_trim_처리")
        void updateConcepts_각_값_trim_처리() {
            LearningFacade facade = createFacade();

            facade.updateConcepts(List.of("  A  ", "  B  "));

            assertThat(facade.getConceptValues()).containsExactly("A", "B");
        }

        @Test
        @DisplayName("updateConcepts_legacy_concept_컬럼도_첫_항목과_동기화")
        void updateConcepts_legacy_concept_컬럼도_첫_항목과_동기화() {
            LearningFacade facade = createFacade();

            facade.updateConcepts(List.of("새로운첫번째", "두번째"));

            assertThat(facade.getConcept()).isEqualTo("새로운첫번째");
        }
    }

    // ─── 예외 ──────────────────────────────────────────────

    @Nested
    @DisplayName("예외")
    class Exceptions {

        @Test
        @DisplayName("updateConcepts_null_예외")
        void updateConcepts_null_예외() {
            LearningFacade facade = createFacade();
            assertThatThrownBy(() -> facade.updateConcepts(null))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("updateConcepts_빈리스트_SIZE_INVALID_예외")
        void updateConcepts_빈리스트_SIZE_INVALID_예외() {
            LearningFacade facade = createFacade();
            assertThatThrownBy(() -> facade.updateConcepts(List.of()))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPTS_SIZE_INVALID);
        }

        @Test
        @DisplayName("updateConcepts_6개_SIZE_INVALID_예외")
        void updateConcepts_6개_SIZE_INVALID_예외() {
            LearningFacade facade = createFacade();
            assertThatThrownBy(() -> facade.updateConcepts(List.of("A", "B", "C", "D", "E", "F")))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPTS_SIZE_INVALID);
        }

        @Test
        @DisplayName("updateConcepts_리스트내_중복_예외_전체_롤백")
        void updateConcepts_리스트내_중복_trim후_예외() {
            LearningFacade facade = createFacade();
            assertThatThrownBy(() -> facade.updateConcepts(List.of("A", "B", "  A  ")))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPT_DUPLICATE);
            // 부분성공 불허 — 기존 concepts 상태 유지
            assertThat(facade.getConceptValues()).containsExactly("백엔드");
        }

        @Test
        @DisplayName("updateConcepts_blank_포함_예외_전체_롤백")
        void updateConcepts_blank_포함_예외_전체_롤백() {
            LearningFacade facade = createFacade();
            assertThatThrownBy(() -> facade.updateConcepts(List.of("A", "  ", "B")))
                    .isInstanceOf(LearningFacadeDomainException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_CONCEPT_BLANK);
            assertThat(facade.getConceptValues()).containsExactly("백엔드");
        }
    }
}
