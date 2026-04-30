package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.LearningMaterial;
import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import com.example.thirdtool.LearningFacade.domain.model.TopicMaterial;
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

@DataJpaTest
@ActiveProfiles("test")
@Import({TopicMaterialRepositoryAdapter.class, QuerydslTestConfig.class})
@DisplayName("TopicMaterialRepository slice")
class TopicMaterialRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    TopicMaterialRepositoryAdapter repository;

    private AxisTopic topic;
    private LearningMaterial material;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        em.persist(user);

        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        em.persist(facade);

        LearningAxis axis = facade.addAxis("API 설계");
        em.flush();

        topic = axis.addTopic("REST API 설계 원칙", null);
        em.flush();

        material = LearningMaterial.create(facade, "DDD 책", MaterialType.TOP_DOWN, "https://e.com/ddd");
        em.persist(material);
        em.flush();
    }

    @Test
    @DisplayName("topicId/materialId 쌍 매핑을 저장하고 countByTopicId·existsByTopicIdAndMaterialId 가 정확하다")
    void save_count_exists() {
        TopicMaterial mapping = TopicMaterial.create(topic, material);
        repository.save(mapping);
        em.flush();
        em.clear();

        assertThat(repository.countByTopicId(topic.getId())).isEqualTo(1);
        assertThat(repository.existsByTopicIdAndMaterialId(topic.getId(), material.getId())).isTrue();
        assertThat(repository.existsByTopicIdAndMaterialId(topic.getId(), 999_999L)).isFalse();
    }

    @Test
    @DisplayName("동일 topicId/materialId 쌍을 두 번 저장하면 unique 제약으로 실패한다")
    void save_duplicate_unique_violation() {
        repository.save(TopicMaterial.create(topic, material));
        em.flush();

        assertThatThrownBy(() -> {
            repository.save(TopicMaterial.create(topic, material));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("MASTERED 자료가 매핑되어 있으면 existsByTopicIdAndMaterialProficiencyLevel(MASTERED)이 true")
    void existsByTopicIdAndMaterialProficiencyLevel_mastered() {
        material.updateProficiencyLevel(ProficiencyLevel.MASTERED);
        repository.save(TopicMaterial.create(topic, material));
        em.flush();
        em.clear();

        boolean hasMastered = repository.existsByTopicIdAndMaterialProficiencyLevel(
                topic.getId(), ProficiencyLevel.MASTERED);
        assertThat(hasMastered).isTrue();
    }
}
