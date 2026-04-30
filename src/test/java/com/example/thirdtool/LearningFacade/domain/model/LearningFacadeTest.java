package com.example.thirdtool.LearningFacade.domain.model;

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

@DisplayName("LearningFacade")
class LearningFacadeTest {

    // ─── 테스트 픽스처 헬퍼 ───────────────────────────────────────────────

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = userWithId(1L);
    }

    private static UserEntity userWithId(Long id) {
        UserEntity entity = UserEntity.ofLocal(
                "tester-" + id, "encoded-pw", "닉네임-" + id, "tester" + id + "@example.com");
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    /** 도메인은 axis.id를 부여하지 않으므로 (DB IDENTITY) 단위 테스트용 ID를 주입한다. */
    private static LearningAxis addAxisWithId(LearningFacade facade, String name, Long id) {
        LearningAxis axis = facade.addAxis(name);
        ReflectionTestUtils.setField(axis, "id", id);
        return axis;
    }

    private LearningFacade createFacade() {
        return LearningFacade.create(user, "백엔드 개발자");
    }

    private LearningFacade createFacadeWithAxes() {
        LearningFacade facade = createFacade();
        facade.addAxis("API 설계");
        facade.addAxis("데이터 모델링");
        facade.addAxis("성능 최적화");
        return facade;
    }

    // ─── 1. 생성 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("유효한 user와 concept으로 생성하면 axes가 비어있다")
        void create_valid() {
            //when
            LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");

            //then
            assertThat(facade.getConcept()).isEqualTo("백엔드 개발자");
            assertThat(facade.getAxes()).isEmpty();
        }

        @Test
        @DisplayName("concept에 앞뒤 공백이 있으면 trim 후 저장한다")
        void create_concept_trim처리() {
            //when
            LearningFacade facade = LearningFacade.create(user, "  백엔드 개발자  ");

            //then
            assertThat(facade.getConcept()).isEqualTo("백엔드 개발자");
        }

        @Test
        @DisplayName("user가 null이면 예외가 발생한다")
        void create_user_null_예외() {
            //when & then
            assertThatThrownBy(() -> LearningFacade.create(null, "백엔드 개발자"))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("concept이 null이면 예외가 발생한다")
        void create_concept_null_예외() {
            //when & then
            assertThatThrownBy(() -> LearningFacade.create(user, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("concept이 공백 문자열이면 예외가 발생한다")
        void create_concept_blank_예외() {
            //when & then
            assertThatThrownBy(() -> LearningFacade.create(user, "   "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 2. 컨셉 수정 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("컨셉 수정")
    class UpdateConcept {

        @Test
        @DisplayName("다른 값으로 수정하면 ConceptChangeRecord.isChanged()가 true이고 concept이 갱신된다")
        void updateConcept_변경값_changed반환() {
            //given
            LearningFacade facade = createFacade();

            //when
            ConceptChangeRecord record = facade.updateConcept("분산 시스템 설계자");

            //then
            assertThat(record.isChanged()).isTrue();
            assertThat(facade.getConcept()).isEqualTo("분산 시스템 설계자");
        }

        @Test
        @DisplayName("동일한 값으로 수정하면 isChanged()가 false이고 concept이 변경되지 않는다")
        void updateConcept_동일값_unchanged반환() {
            //given
            LearningFacade facade = createFacade();
            String originalConcept = facade.getConcept();

            //when
            ConceptChangeRecord record = facade.updateConcept("백엔드 개발자");

            //then
            assertThat(record.isChanged()).isFalse();
            assertThat(facade.getConcept()).isEqualTo(originalConcept);
        }

        @Test
        @DisplayName("동일한 값으로 수정하면 updatedAt이 변경되지 않는다")
        void updateConcept_동일값_updatedAt_불변() {
            //given
            LearningFacade facade = createFacade();
            java.time.LocalDateTime updatedAtBefore = facade.getUpdatedAt();

            //when
            facade.updateConcept("백엔드 개발자");

            //then
            assertThat(facade.getUpdatedAt()).isEqualTo(updatedAtBefore);
        }

        @Test
        @DisplayName("컨셉 수정 후에도 기존 axes 목록이 그대로 유지된다")
        void updateConcept_후_axes_보존() {
            //given
            LearningFacade facade = createFacadeWithAxes();
            int axisCountBefore = facade.getAxes().size();

            //when
            facade.updateConcept("분산 시스템 설계자");

            //then
            assertThat(facade.getAxes()).hasSize(axisCountBefore);
        }

        @Test
        @DisplayName("앞뒤 공백 포함 동일 값으로 수정하면 trim 후 비교해 isChanged()가 false를 반환한다")
        void updateConcept_trim처리_동일값판단() {
            //given
            LearningFacade facade = createFacade();    // concept = "백엔드 개발자"

            //when
            ConceptChangeRecord record = facade.updateConcept("  백엔드 개발자  ");

            //then
            assertThat(record.isChanged()).isFalse();
        }

        @Test
        @DisplayName("공백 문자열로 수정하면 예외가 발생한다")
        void updateConcept_blank_예외() {
            //given
            LearningFacade facade = createFacade();

            //when & then
            assertThatThrownBy(() -> facade.updateConcept("  "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("null로 수정하면 예외가 발생한다")
        void updateConcept_null_예외() {
            //given
            LearningFacade facade = createFacade();

            //when & then
            assertThatThrownBy(() -> facade.updateConcept(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 3. 축 추가 ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("축 추가")
    class AddAxis {

        @Test
        @DisplayName("axes가 빈 상태에서 첫 번째 축을 추가하면 displayOrder가 1이다")
        void addAxis_첫번째_displayOrder_1() {
            //given
            LearningFacade facade = createFacade();

            //when
            LearningAxis axis = facade.addAxis("API 설계");

            //then
            assertThat(axis.getDisplayOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("이미 1개 존재하는 상태에서 추가하면 displayOrder가 2이다")
        void addAxis_두번째_displayOrder_마지막플러스1() {
            //given
            LearningFacade facade = createFacade();
            facade.addAxis("API 설계");

            //when
            LearningAxis axis = facade.addAxis("데이터 모델링");

            //then
            assertThat(axis.getDisplayOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("새로 추가된 축의 actions 목록은 비어있다")
        void addAxis_반환된axis_actions_비어있음() {
            //given
            LearningFacade facade = createFacade();

            //when
            LearningAxis axis = facade.addAxis("API 설계");

            //then
            assertThat(axis.getTopics()).isEmpty();
        }

        @Test
        @DisplayName("동일한 Facade 내에서 이미 존재하는 이름으로 축을 추가하면 예외가 발생한다")
        void addAxis_중복이름_예외() {
            //given
            LearningFacade facade = createFacade();
            facade.addAxis("API 설계");

            //when & then
            assertThatThrownBy(() -> facade.addAxis("API 설계"))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("축 이름이 공백 문자열이면 예외가 발생한다")
        void addAxis_name_blank_예외() {
            //given
            LearningFacade facade = createFacade();

            //when & then
            assertThatThrownBy(() -> facade.addAxis("  "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("축 이름이 null이면 예외가 발생한다")
        void addAxis_name_null_예외() {
            //given
            LearningFacade facade = createFacade();

            //when & then
            assertThatThrownBy(() -> facade.addAxis(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 4. 축 삭제 ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("축 삭제")
    class RemoveAxis {

        @Test
        @DisplayName("존재하는 axisId로 삭제하면 axes 컬렉션에서 제거된다")
        void removeAxis_valid() {
            //given
            LearningFacade facade = createFacade();
            LearningAxis axis = addAxisWithId(facade, "API 설계", 10L);
            Long axisId = axis.getId();

            //when
            facade.removeAxis(axisId);

            //then
            assertThat(facade.getAxes()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 axisId로 삭제하면 예외가 발생한다")
        void removeAxis_존재하지않는axisId_예외() {
            //given
            LearningFacade facade = createFacade();

            //when & then
            assertThatThrownBy(() -> facade.removeAxis(999L))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 5. 축 순서 변경 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("축 순서 변경")
    class ReorderAxes {

        @Test
        @DisplayName("전달된 id 순서대로 displayOrder가 1부터 재부여된다")
        void reorderAxes_valid_순서재부여() {
            //given
            LearningFacade facade = createFacade();
            LearningAxis axis1 = addAxisWithId(facade, "API 설계", 10L);
            LearningAxis axis2 = addAxisWithId(facade, "데이터 모델링", 11L);
            LearningAxis axis3 = addAxisWithId(facade, "성능 최적화", 12L);

            //when
            facade.reorderAxes(List.of(axis3.getId(), axis1.getId(), axis2.getId()));

            //then
            assertThat(axis3.getDisplayOrder()).isEqualTo(1);
            assertThat(axis1.getDisplayOrder()).isEqualTo(2);
            assertThat(axis2.getDisplayOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("axes가 없고 빈 리스트를 전달하면 정상 처리된다")
        void reorderAxes_빈목록_빈축집합_정상() {
            //given
            LearningFacade facade = createFacade();

            //when & then
            facade.reorderAxes(List.of());   // 예외 없이 통과
        }

        @Test
        @DisplayName("전달된 id 수가 현재 axes 수와 다르면 예외가 발생한다")
        void reorderAxes_id집합_불일치_예외() {
            //given
            LearningFacade facade = createFacade();
            LearningAxis axis1 = addAxisWithId(facade, "API 설계", 10L);
            LearningAxis axis2 = addAxisWithId(facade, "데이터 모델링", 11L);
            addAxisWithId(facade, "성능 최적화", 12L);

            //when & then — 3개 중 2개만 전달
            assertThatThrownBy(() -> facade.reorderAxes(List.of(axis1.getId(), axis2.getId())))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("현재 axes에 없는 id가 목록에 포함되면 예외가 발생한다")
        void reorderAxes_존재하지않는id포함_예외() {
            //given
            LearningFacade facade = createFacade();
            LearningAxis axis1 = addAxisWithId(facade, "API 설계", 10L);
            LearningAxis axis2 = addAxisWithId(facade, "데이터 모델링", 11L);

            //when & then — 유령 id 999L 포함
            assertThatThrownBy(() -> facade.reorderAxes(List.of(axis1.getId(), axis2.getId(), 999L)))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    // ─── 6. 보조 조회 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("보조 조회")
    class HelperQuery {

        @Test
        @DisplayName("소유 userId를 전달하면 isOwnedBy가 true를 반환한다")
        void isOwnedBy_소유자_true() {
            //given
            LearningFacade facade = createFacade();    // user.id = 1L

            //when & then
            assertThat(facade.isOwnedBy(1L)).isTrue();
        }

        @Test
        @DisplayName("다른 userId를 전달하면 isOwnedBy가 false를 반환한다")
        void isOwnedBy_다른유저_false() {
            //given
            LearningFacade facade = createFacade();

            //when & then
            assertThat(facade.isOwnedBy(999L)).isFalse();
        }

        @Test
        @DisplayName("axes가 5개 이하이면 isAxisCountExceedsRecommended가 false를 반환한다")
        void isAxisCountExceedsRecommended_5개이하_false() {
            //given
            LearningFacade facade = createFacade();
            for (int i = 1; i <= 5; i++) {
                facade.addAxis("축 " + i);
            }

            //when & then
            assertThat(facade.isAxisCountExceedsRecommended()).isFalse();
        }

        @Test
        @DisplayName("axes가 6개이면 isAxisCountExceedsRecommended가 true를 반환한다 — 경계값")
        void isAxisCountExceedsRecommended_6개_true() {
            //given
            LearningFacade facade = createFacade();
            for (int i = 1; i <= 6; i++) {
                facade.addAxis("축 " + i);
            }

            //when & then
            assertThat(facade.isAxisCountExceedsRecommended()).isTrue();
        }

        @Test
        @DisplayName("axes가 0개이면 isAxisCountExceedsRecommended가 false를 반환한다")
        void isAxisCountExceedsRecommended_0개_false() {
            //given
            LearningFacade facade = createFacade();

            //when & then
            assertThat(facade.isAxisCountExceedsRecommended()).isFalse();
        }
    }
}