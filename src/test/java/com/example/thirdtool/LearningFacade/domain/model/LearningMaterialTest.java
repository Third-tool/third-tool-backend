package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningMaterial")
class LearningMaterialTest {

    // ─── 테스트 픽스처 헬퍼 ───────────────────────────────────────────────

    private LearningFacade facade;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        facade = LearningFacade.create(user, "백엔드 개발자");
    }

    private LearningMaterial createMaterial() {
        return LearningMaterial.create(facade, "도메인 주도 설계", MaterialType.TOP_DOWN, "https://example.com/ddd");
    }

    // ─── 1. 생성 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("유효한 파라미터로 생성하면 proficiencyLevel이 UNRATED로 초기화된다")
        void create_valid_proficiencyLevel_UNRATED() {
            //when
            LearningMaterial material = LearningMaterial.create(
                    facade, "도메인 주도 설계", MaterialType.TOP_DOWN, "https://example.com/ddd");

            //then
            assertThat(material.getProficiencyLevel()).isEqualTo(ProficiencyLevel.UNRATED);
        }

        @Test
        @DisplayName("url이 null이어도 정상적으로 생성된다 — v1 선택 항목")
        void create_url_null_허용() {
            //when
            LearningMaterial material = LearningMaterial.create(
                    facade, "도메인 주도 설계", MaterialType.TOP_DOWN, null);

            //then
            assertThat(material.getUrl()).isNull();
        }

        @Test
        @DisplayName("생성 직후 actionMappings가 비어있다")
        void create_actionMappings_비어있음() {
            //when
            LearningMaterial material = createMaterial();

            //then
            assertThat(material.getActionMappings()).isEmpty();
        }

        @Test
        @DisplayName("facade가 null이면 예외가 발생한다")
        void create_facade_null_예외() {
            //when & then
            assertThatThrownBy(() ->
                    LearningMaterial.create(null, "도메인 주도 설계", MaterialType.TOP_DOWN, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("name이 null이면 예외가 발생한다")
        void create_name_null_예외() {
            //when & then
            assertThatThrownBy(() ->
                    LearningMaterial.create(facade, null, MaterialType.TOP_DOWN, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("name이 공백 문자열이면 예외가 발생한다")
        void create_name_blank_예외() {
            //when & then
            assertThatThrownBy(() ->
                    LearningMaterial.create(facade, "   ", MaterialType.TOP_DOWN, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("materialType이 null이면 예외가 발생한다")
        void create_materialType_null_예외() {
            //when & then
            assertThatThrownBy(() ->
                    LearningMaterial.create(facade, "도메인 주도 설계", null, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 2. 자료명 수정 ──────────────────────────────────────────────────

    @Nested
    @DisplayName("자료명 수정")
    class UpdateName {

        @Test
        @DisplayName("유효한 이름으로 수정하면 name이 갱신된다")
        void updateName_valid() {
            //given
            LearningMaterial material = createMaterial();

            //when
            material.updateName("도메인 주도 설계 (Eric Evans)");

            //then
            assertThat(material.getName()).isEqualTo("도메인 주도 설계 (Eric Evans)");
        }

        @Test
        @DisplayName("공백 문자열로 수정하면 예외가 발생한다")
        void updateName_blank_예외() {
            //given
            LearningMaterial material = createMaterial();

            //when & then
            assertThatThrownBy(() -> material.updateName("  "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("null로 수정하면 예외가 발생한다")
        void updateName_null_예외() {
            //given
            LearningMaterial material = createMaterial();

            //when & then
            assertThatThrownBy(() -> material.updateName(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 3. 숙련도 변경 ──────────────────────────────────────────────────

    @Nested
    @DisplayName("숙련도 변경")
    class UpdateProficiencyLevel {

        @Test
        @DisplayName("UNRATED → UNFAMILIAR 변경이 정상적으로 적용된다 — 슬라이더 첫 조작")
        void updateProficiencyLevel_UNRATED_UNFAMILIAR() {
            //given
            LearningMaterial material = createMaterial();   // 초기: UNRATED

            //when
            material.updateProficiencyLevel(ProficiencyLevel.UNFAMILIAR);

            //then
            assertThat(material.getProficiencyLevel()).isEqualTo(ProficiencyLevel.UNFAMILIAR);
        }

        @Test
        @DisplayName("UNFAMILIAR → GETTING_USED 변경이 정상적으로 적용된다")
        void updateProficiencyLevel_UNFAMILIAR_GETTING_USED() {
            //given
            LearningMaterial material = createMaterial();
            material.updateProficiencyLevel(ProficiencyLevel.UNFAMILIAR);

            //when
            material.updateProficiencyLevel(ProficiencyLevel.GETTING_USED);

            //then
            assertThat(material.getProficiencyLevel()).isEqualTo(ProficiencyLevel.GETTING_USED);
        }

        @Test
        @DisplayName("GETTING_USED → MASTERED 변경이 정상적으로 적용된다 — 커버리지 재계산 트리거 근거")
        void updateProficiencyLevel_GETTING_USED_MASTERED() {
            //given
            LearningMaterial material = createMaterial();
            material.updateProficiencyLevel(ProficiencyLevel.GETTING_USED);

            //when
            material.updateProficiencyLevel(ProficiencyLevel.MASTERED);

            //then
            assertThat(material.getProficiencyLevel()).isEqualTo(ProficiencyLevel.MASTERED);
        }

        @Test
        @DisplayName("null로 변경하면 예외가 발생한다")
        void updateProficiencyLevel_null_예외() {
            //given
            LearningMaterial material = createMaterial();

            //when & then
            assertThatThrownBy(() -> material.updateProficiencyLevel(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }
}