package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.ActionMaterial;
import com.example.thirdtool.LearningFacade.domain.model.AxisAction;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.LearningMaterial;
import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.support.QuerydslTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ActionMaterialRepository slice 테스트 (@DataJpaTest + H2).
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({ActionMaterialRepositoryAdapter.class, QuerydslTestConfig.class})
@DisplayName("ActionMaterialRepository slice")
class ActionMaterialRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ActionMaterialRepositoryAdapter repository;

    private AxisAction action;
    private LearningMaterial material;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        em.persist(user);

        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        em.persist(facade);

        LearningAxis axis = facade.addAxis("API 설계");
        em.persist(axis);

        action = axis.addAction("설계하다");
        em.persist(action);

        material = LearningMaterial.create(facade, "DDD 책", MaterialType.TOP_DOWN, "https://example.com/ddd");
        em.persist(material);

        em.flush();
    }

    @Test
    @DisplayName("S20 변형: 매핑 존재 시 existsByActionIdAndMaterialId가 true를 반환한다")
    void existsByActionIdAndMaterialId_매핑존재_true() {
        // given
        ActionMaterial mapping = ActionMaterial.create(action, material);
        em.persist(mapping);
        em.flush();

        // when
        boolean exists = repository.existsByActionIdAndMaterialId(action.getId(), material.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("S20 변형: 매핑 없을 때 existsByActionIdAndMaterialId가 false를 반환한다")
    void existsByActionIdAndMaterialId_매핑없음_false() {
        // when
        boolean exists = repository.existsByActionIdAndMaterialId(action.getId(), material.getId());

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("S21 변형: countByActionId는 해당 행동의 매핑 수를 반환한다")
    void countByActionId_매핑수반환() {
        // given
        ActionMaterial mapping = ActionMaterial.create(action, material);
        em.persist(mapping);
        em.flush();

        // when
        long count = repository.countByActionId(action.getId());

        // then
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("S22 변형: 동일 (action, material) 쌍을 두 번 저장하면 uk_action_material 제약 위반")
    void uk_action_material_위반() {
        // given
        ActionMaterial first = ActionMaterial.create(action, material);
        em.persist(first);
        em.flush();

        // when & then — DB UNIQUE 제약이 이중 매핑을 차단한다.
        // Spring repository.save()는 SpringDataExceptionTranslation을 적용해 DataIntegrityViolationException으로 감싼다.
        assertThatThrownBy(() -> {
            ActionMaterial duplicate = ActionMaterial.create(action, material);
            repository.save(duplicate);
            em.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, jakarta.persistence.PersistenceException.class);
    }

    @Test
    @DisplayName("S26 변형: 매핑 저장 시 linkedAt이 자동 기록된다")
    void persist_linkedAt_자동기록() {
        // given
        ActionMaterial mapping = ActionMaterial.create(action, material);

        // when
        em.persist(mapping);
        em.flush();

        // then
        assertThat(mapping.getLinkedAt()).isNotNull();
    }
}
