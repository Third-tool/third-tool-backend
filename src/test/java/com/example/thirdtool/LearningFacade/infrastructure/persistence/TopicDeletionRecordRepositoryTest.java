package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.TopicDeletionRecord;
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
@Import({TopicDeletionRecordRepositoryAdapter.class, QuerydslTestConfig.class})
@DisplayName("TopicDeletionRecordRepository slice")
class TopicDeletionRecordRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    TopicDeletionRecordRepositoryAdapter repository;

    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-deletion", "encoded-pw", "닉네임-deletion", "del@example.com");
        em.persist(user);

        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        em.persist(facade);
        axis = facade.addAxis("API 설계");
        em.flush();
    }

    private AxisTopic persistTopic(String name) {
        AxisTopic topic = axis.addTopic(name, null);
        em.flush();
        return topic;
    }

    @Test
    @DisplayName("저장 후 동일 축 id로 조회 시 deletedAt 내림차순(최신순)으로 반환")
    void save_and_findByLearningAxisIdOrderByDeletedAtDesc() throws InterruptedException {
        AxisTopic t1 = persistTopic("주제1");
        repository.save(TopicDeletionRecord.of(t1));
        Thread.sleep(5);
        AxisTopic t2 = persistTopic("주제2");
        repository.save(TopicDeletionRecord.of(t2));
        Thread.sleep(5);
        AxisTopic t3 = persistTopic("주제3");
        repository.save(TopicDeletionRecord.of(t3));
        em.flush();
        em.clear();

        List<TopicDeletionRecord> result = repository.findByLearningAxisIdOrderByDeletedAtDesc(axis.getId());

        assertThat(result)
                .hasSize(3)
                .extracting(TopicDeletionRecord::getName)
                .containsExactly("주제3", "주제2", "주제1");
    }

    @Test
    @DisplayName("삭제 이력이 없는 축은 빈 목록")
    void findByLearningAxisIdOrderByDeletedAtDesc_빈목록() {
        List<TopicDeletionRecord> result = repository.findByLearningAxisIdOrderByDeletedAtDesc(axis.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("원본 axis_topic이 사라져도 archive 레코드는 보존된다 (FK 없음)")
    void archive_survives_after_topic_removed() {
        AxisTopic topic = persistTopic("사라질 주제");
        TopicDeletionRecord saved = repository.save(TopicDeletionRecord.of(topic));
        Long savedId = saved.getId();

        // 원본 주제 제거 (cascade로 axis_topic에서 사라짐)
        axis.removeTopic(topic.getId());
        em.flush();
        em.clear();

        TopicDeletionRecord found = em.find(TopicDeletionRecord.class, savedId);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("사라질 주제");
    }
}
