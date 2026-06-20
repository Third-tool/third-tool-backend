package com.example.thirdtool.LearningFacade.integration;

import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeQueryService;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * product-learningFacade.md Story 2-3 + 3-3 — getFacade 응답에 isFocused·isRefinementSuggested
 * 두 flag가 정합하게 노출되는지 통합 검증.
 *
 * <p>단위 테스트는 도메인 boolean을 직접 확인하지만, DTO mapper가 LearningAxis.FOCUS_TOP_N(=3)
 * 상수를 올바르게 전달하는지·복수 topic을 정렬 + flag 매핑까지 컨텍스트에서 정합한지는 통합 테스트
 * 범위.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("LearningFacade getFacade 응답 — isFocused / isRefinementSuggested 통합")
class LearningFacadeQueryFocusedFlagsIntegrationTest {

    @Autowired LearningFacadeQueryService learningFacadeQueryService;

    @PersistenceContext EntityManager em;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("owner", "encoded-pw", "닉네임", "owner@example.com");
        em.persist(user);
        em.flush();
    }

    @Test
    @DisplayName("주제 5개 — 상위 3개만 isFocused=true (FOCUS_TOP_N=3 적용)")
    void focusFlag_topThreeOfFive() {
        // given — facade 1개 + axis 1개 + topic 5개
        LearningFacade facade = LearningFacade.create(user, "백엔드");
        LearningAxis axis = facade.addAxis("Spring");
        for (int i = 1; i <= 5; i++) {
            axis.addTopic("주제 " + i, null);
        }
        em.persist(facade);
        em.flush();
        em.clear();

        // when
        LearningFacadeResponse.FacadeDetail response = learningFacadeQueryService.getFacade(
                new LearningFacadeQuery.GetFacade(user.getId())
        );

        // then — displayOrder 1·2·3은 focused, 4·5는 not
        List<LearningFacadeResponse.TopicItem> topics = response.axes().get(0).topics();
        assertThat(topics).hasSize(5);
        assertThat(topics).extracting(LearningFacadeResponse.TopicItem::isFocused)
                .containsExactly(true, true, true, false, false);
    }

    @Test
    @DisplayName("주제 2개 (3개 미만) — 모든 주제가 isFocused=true (Spec 엣지 케이스)")
    void focusFlag_allFocusedWhenFewerThanThree() {
        LearningFacade facade = LearningFacade.create(user, "백엔드");
        LearningAxis axis = facade.addAxis("Spring");
        axis.addTopic("주제 1", null);
        axis.addTopic("주제 2", null);
        em.persist(facade);
        em.flush();
        em.clear();

        LearningFacadeResponse.FacadeDetail response = learningFacadeQueryService.getFacade(
                new LearningFacadeQuery.GetFacade(user.getId())
        );

        List<LearningFacadeResponse.TopicItem> topics = response.axes().get(0).topics();
        assertThat(topics).extracting(LearningFacadeResponse.TopicItem::isFocused)
                .containsExactly(true, true);
    }

    @Test
    @DisplayName("이름 3회 수정한 주제는 isRefinementSuggested=true (REFINEMENT_THRESHOLD=3 적용)")
    void refinementFlag_afterThreeNameUpdates() {
        LearningFacade facade = LearningFacade.create(user, "백엔드");
        LearningAxis axis = facade.addAxis("Spring");
        AxisTopic topic = axis.addTopic("v0", null);
        topic.updateName("v1");
        topic.updateName("v2");
        topic.updateName("v3");
        em.persist(facade);
        em.flush();
        em.clear();

        LearningFacadeResponse.FacadeDetail response = learningFacadeQueryService.getFacade(
                new LearningFacadeQuery.GetFacade(user.getId())
        );

        LearningFacadeResponse.TopicItem topicItem = response.axes().get(0).topics().get(0);
        assertThat(topicItem.isRefinementSuggested()).isTrue();
    }

    @Test
    @DisplayName("이름 미수정 신규 주제는 isRefinementSuggested=false")
    void refinementFlag_freshTopicFalse() {
        LearningFacade facade = LearningFacade.create(user, "백엔드");
        LearningAxis axis = facade.addAxis("Spring");
        axis.addTopic("새 주제", null);
        em.persist(facade);
        em.flush();
        em.clear();

        LearningFacadeResponse.FacadeDetail response = learningFacadeQueryService.getFacade(
                new LearningFacadeQuery.GetFacade(user.getId())
        );

        LearningFacadeResponse.TopicItem topicItem = response.axes().get(0).topics().get(0);
        assertThat(topicItem.isRefinementSuggested()).isFalse();
        assertThat(topicItem.isFocused()).isTrue(); // displayOrder=1
    }
}
