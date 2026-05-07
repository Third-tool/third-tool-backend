package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RevisionReasonOption")
class RevisionReasonOptionTest {

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("정상 생성 — label·displayOrder·active 보존")
        void create_valid() {
            RevisionReasonOption option = RevisionReasonOption.of(
                    "더 정확한 표현을 찾았다", 3, true);

            assertThat(option.getLabel()).isEqualTo("더 정확한 표현을 찾았다");
            assertThat(option.getDisplayOrder()).isEqualTo(3);
            assertThat(option.isActive()).isTrue();
        }

        @Test
        @DisplayName("label은 trim 후 저장된다")
        void create_label_trim() {
            RevisionReasonOption option = RevisionReasonOption.of("  방향 자체가 바뀌었다  ", 1, true);
            assertThat(option.getLabel()).isEqualTo("방향 자체가 바뀌었다");
        }

        @Test
        @DisplayName("label이 null 또는 blank면 예외")
        void create_label_blank_예외() {
            assertThatThrownBy(() -> RevisionReasonOption.of("  ", 1, true))
                    .isInstanceOf(LearningFacadeDomainException.class);
            assertThatThrownBy(() -> RevisionReasonOption.of(null, 1, true))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("displayOrder가 1 미만이면 예외")
        void create_displayOrder_under_1_예외() {
            assertThatThrownBy(() -> RevisionReasonOption.of("이유", 0, true))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    @Nested
    @DisplayName("isUsableForNewRevision")
    class UsableForNewRevision {

        @Test
        @DisplayName("active=true면 신규 이력 생성에 사용 가능")
        void active_true_usable() {
            RevisionReasonOption option = RevisionReasonOption.of("이유", 1, true);
            assertThat(option.isUsableForNewRevision()).isTrue();
        }

        @Test
        @DisplayName("active=false면 신규 이력 생성에 사용 불가")
        void active_false_not_usable() {
            RevisionReasonOption option = RevisionReasonOption.of("이유", 1, false);
            assertThat(option.isUsableForNewRevision()).isFalse();
        }
    }
}
