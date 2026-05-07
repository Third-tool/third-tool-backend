package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.TopicRevision;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.support.QuerydslTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({TopicRevisionRepositoryAdapter.class, QuerydslTestConfig.class})
@DisplayName("TopicRevisionRepository slice")
class TopicRevisionRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    TopicRevisionRepositoryAdapter repository;

    private AxisTopic topic;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-rev", "encoded-pw", "닉네임-rev", "rev@example.com");
        em.persist(user);

        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        em.persist(facade);
        LearningAxis axis = facade.addAxis("API 설계");
        topic = axis.addTopic("REST API 설계 원칙", null);
        em.flush();
    }

    @Test
    @DisplayName("주제별 이력을 revisedAt 오름차순으로 반환한다")
    void findByTopicIdOrderByRevisedAtAsc_정렬() {
        repository.save(TopicRevision.of(topic, "REST API 설계 원칙", "REST API 설계 가이드", "더 정확한 표현을 찾았다"));
        repository.save(TopicRevision.of(topic, "REST API 설계 가이드", "REST 아키텍처 가이드", null));
        em.flush();
        em.clear();

        List<TopicRevision> result = repository.findByTopicIdOrderByRevisedAtAsc(topic.getId());

        assertThat(result)
                .hasSize(2)
                .extracting(TopicRevision::getNewName)
                .containsExactly("REST API 설계 가이드", "REST 아키텍처 가이드");
    }

    @Test
    @DisplayName("이력이 없는 주제는 빈 목록을 반환한다")
    void findByTopicIdOrderByRevisedAtAsc_빈목록() {
        List<TopicRevision> result = repository.findByTopicIdOrderByRevisedAtAsc(topic.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("revisionReasonLabel은 nullable로 저장 가능")
    void save_reasonLabel_null() {
        TopicRevision saved = repository.save(TopicRevision.of(topic, "이전", "이후", null));
        em.flush();
        em.clear();

        TopicRevision loaded = repository.findByTopicIdOrderByRevisedAtAsc(topic.getId()).get(0);
        assertThat(loaded.getRevisionReasonLabel()).isNull();
    }
}
