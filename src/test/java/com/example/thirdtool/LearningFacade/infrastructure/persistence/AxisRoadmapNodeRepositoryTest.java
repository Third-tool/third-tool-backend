package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisRoadmapNode;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
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

/**
 * Story-LT-E3-S3-8 · AxisRoadmapNode Repository Slice.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({AxisRoadmapNodeRepositoryAdapter.class, QuerydslTestConfig.class})
@DisplayName("AxisRoadmapNodeRepository slice (Story-LT-E3-S3-8)")
class AxisRoadmapNodeRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    AxisRoadmapNodeRepositoryAdapter repository;

    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        em.persist(user);

        LearningFacade facade = LearningFacade.create(user, "백엔드");
        em.persist(facade);
        axis = facade.addAxis("하네스 엔지니어링");
        em.flush();
    }

    @Test
    @DisplayName("axisId로 조회하면 displayOrder 오름차순으로 반환")
    void findByAxisIdOrderByDisplayOrderAsc() {
        axis.addRoadmapNode("1. 기초", null, "├── 1-1. ...");
        axis.addRoadmapNode("2. 심화", null, "├── 2-1. ...");
        axis.addRoadmapNode("3. 실전", null, "├── 3-1. ...");
        em.flush();
        em.clear();

        List<AxisRoadmapNode> result = repository.findByAxisIdOrderByDisplayOrderAsc(axis.getId());

        assertThat(result)
                .extracting(AxisRoadmapNode::getTitle)
                .containsExactly("1. 기초", "2. 심화", "3. 실전");
        assertThat(result)
                .extracting(AxisRoadmapNode::getDisplayOrder)
                .containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("softDelete된 노드는 @SQLRestriction으로 자동 제외")
    void softDeleted_filteredOut() {
        AxisRoadmapNode n1 = axis.addRoadmapNode("1. 기초", null, "b1");
        AxisRoadmapNode n2 = axis.addRoadmapNode("2. 심화", null, "b2");
        em.flush();
        Long deletedId = n1.getId();
        Long remainingId = n2.getId();

        n1.softDelete();
        em.flush();
        em.clear();

        List<AxisRoadmapNode> result = repository.findByAxisIdOrderByDisplayOrderAsc(axis.getId());
        assertThat(result).extracting(AxisRoadmapNode::getId).containsExactly(remainingId);
        assertThat(repository.findById(deletedId)).isEmpty();
    }

    @Test
    @DisplayName("save 시 audit 자동 기록 (created_at + updated_at)")
    void save_setsAuditColumns() {
        AxisRoadmapNode node = axis.addRoadmapNode("1. 기초", "근거", "├── 1-1. ...");
        em.flush();
        em.clear();

        AxisRoadmapNode found = repository.findById(node.getId()).orElseThrow();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getDeletedAt()).isNull();
        assertThat(found.getRationale()).isEqualTo("근거");
    }
}
