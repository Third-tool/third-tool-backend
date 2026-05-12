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
        return LearningMaterial.create(facade, "도메인 주도 설계", MaterialType.BOOK, "https://example.com/ddd");
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
                    facade, "도메인 주도 설계", MaterialType.BOOK, "https://example.com/ddd");

            //then
            assertThat(material.getProficiencyLevel()).isEqualTo(ProficiencyLevel.UNRATED);
        }

        @Test
        @DisplayName("url이 null이어도 정상적으로 생성된다 — v1 선택 항목")
        void create_url_null_허용() {
            //when
            LearningMaterial material = LearningMaterial.create(
                    facade, "도메인 주도 설계", MaterialType.BOOK, null);

            //then
            assertThat(material.getUrl()).isNull();
        }

        @Test
        @DisplayName("생성 직후 topicMappings가 비어있다")
        void create_topicMappings_비어있음() {
            //when
            LearningMaterial material = createMaterial();

            //then
            assertThat(material.getTopicMappings()).isEmpty();
        }

        @Test
        @DisplayName("facade가 null이면 예외가 발생한다")
        void create_facade_null_예외() {
            //when & then
            assertThatThrownBy(() ->
                    LearningMaterial.create(null, "도메인 주도 설계", MaterialType.BOOK, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("name이 null이면 예외가 발생한다")
        void create_name_null_예외() {
            //when & then
            assertThatThrownBy(() ->
                    LearningMaterial.create(facade, null, MaterialType.BOOK, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("name이 공백 문자열이면 예외가 발생한다")
        void create_name_blank_예외() {
            //when & then
            assertThatThrownBy(() ->
                    LearningMaterial.create(facade, "   ", MaterialType.BOOK, null))
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

    // ─── 부가 속성 5종 정규화 (Story-003-4) ────────────────────────────────

    @Nested
    @DisplayName("부가 속성 5종 (author/platform/aiProvider/webSource/memo)")
    class OptionalAttributes {

        @Test
        @DisplayName("BOOK 등록 시 author/platform/aiProvider/webSource/memo가 그대로 보존된다")
        void create_with_optional_attributes_all_preserved() {
            //when
            LearningMaterial material = LearningMaterial.create(
                    facade,
                    "Real MySQL 8.0",
                    MaterialType.BOOK,
                    "https://example.com/real-mysql",
                    "백은빈, 이성욱",
                    "인프런",
                    "Claude",
                    "Notion",
                    "인덱스 챕터 위주로 참조"
            );

            //then
            assertThat(material.getAuthor()).isEqualTo("백은빈, 이성욱");
            assertThat(material.getPlatform()).isEqualTo("인프런");
            assertThat(material.getAiProvider()).isEqualTo("Claude");
            assertThat(material.getWebSource()).isEqualTo("Notion");
            assertThat(material.getMemo()).isEqualTo("인덱스 챕터 위주로 참조");
        }

        @Test
        @DisplayName("부가 속성 5종 미입력 시(create 4-arg 편의 팩토리) 모두 null로 시작한다")
        void create_optional_attributes_omitted_all_null() {
            //when
            LearningMaterial material = LearningMaterial.create(
                    facade, "Real MySQL 8.0", MaterialType.BOOK, "https://example.com/real-mysql");

            //then
            assertThat(material.getAuthor()).isNull();
            assertThat(material.getPlatform()).isNull();
            assertThat(material.getAiProvider()).isNull();
            assertThat(material.getWebSource()).isNull();
            assertThat(material.getMemo()).isNull();
        }

        @Test
        @DisplayName("부가 속성에 양쪽 공백이 포함되면 trim된다")
        void create_optional_attributes_trim() {
            //when
            LearningMaterial material = LearningMaterial.create(
                    facade, "Real MySQL", MaterialType.BOOK, null,
                    "  백은빈  ", "  인프런  ", "  Claude  ", "  Notion  ", "  메모  ");

            //then
            assertThat(material.getAuthor()).isEqualTo("백은빈");
            assertThat(material.getPlatform()).isEqualTo("인프런");
            assertThat(material.getAiProvider()).isEqualTo("Claude");
            assertThat(material.getWebSource()).isEqualTo("Notion");
            assertThat(material.getMemo()).isEqualTo("메모");
        }

        @Test
        @DisplayName("부가 속성이 빈 문자열·공백뿐이면 null로 정규화된다 (domain-conventions §1)")
        void create_optional_attributes_blank_normalized_to_null() {
            //when
            LearningMaterial material = LearningMaterial.create(
                    facade, "Real MySQL", MaterialType.BOOK, "",
                    "", "   ", "", "  ", "");

            //then
            assertThat(material.getUrl()).isNull();
            assertThat(material.getAuthor()).isNull();
            assertThat(material.getPlatform()).isNull();
            assertThat(material.getAiProvider()).isNull();
            assertThat(material.getWebSource()).isNull();
            assertThat(material.getMemo()).isNull();
        }

        @Test
        @DisplayName("타입과 부가 속성의 정합성은 도메인이 강제하지 않는다 — BOOK에 platform 입력해도 저장됨 (귀속 모호 §8 디폴트)")
        void create_type_attribute_mismatch_not_blocked() {
            //when — BOOK인데 platform·aiProvider 입력
            LearningMaterial material = LearningMaterial.create(
                    facade, "Real MySQL", MaterialType.BOOK, null,
                    "백은빈", "인프런", "Claude", null, null);

            //then — 도메인 통과, 저장됨
            assertThat(material.getMaterialType()).isEqualTo(MaterialType.BOOK);
            assertThat(material.getPlatform()).isEqualTo("인프런");
            assertThat(material.getAiProvider()).isEqualTo("Claude");
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