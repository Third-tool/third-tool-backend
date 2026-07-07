package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeCommand;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicDeletionRecordRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicRevisionRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.AddAxis;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("LearningFacadeCommandService.addAxis() — 축 추가 (Deck 동기화 제거 후 잔여 계약)")
class LearningFacadeCommandServiceAddAxisTest {

    private LearningFacadeRepository facadeRepository;
    private LearningFacadeCommandService service;

    private UserEntity user;
    private LearningFacade facade;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);

        service = new LearningFacadeCommandService(
                facadeRepository,
                mock(TopicRevisionRepository.class),
                mock(RevisionReasonOptionRepository.class),
                mock(TopicDeletionRecordRepository.class),
                mock(LearningMaterialRepository.class),
                mock(TopicMaterialRepository.class));

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        facade = LearningFacade.create(user, "백엔드 개발자");
        ReflectionTestUtils.setField(facade, "id", 50L);

        when(facadeRepository.findByUserId(1L)).thenReturn(Optional.of(facade));
        when(facadeRepository.save(any(LearningFacade.class))).thenAnswer(inv -> {
            LearningFacade saved = inv.getArgument(0);
            // JPA cascade 모사 — 새로 추가된 마지막 axis에 id 부여
            saved.getAxes().stream()
                    .filter(a -> a.getId() == null)
                    .forEach(a -> ReflectionTestUtils.setField(a, "id", 100L));
            return saved;
        });
    }

    // ─── 해피 ────────────────────────────────────────────────────

    @Test
    @DisplayName("addAxis_정상_axis추가및저장 — 응답에 axisId·name·displayOrder 포함, facade 저장됨")
    void addAxis_정상_axis추가및저장() {
        AddAxis response = service.addAxis(new LearningFacadeCommand.AddAxis(1L, "Java 심화"));

        assertThat(response.axisId()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("Java 심화");
        assertThat(response.displayOrder()).isEqualTo(1);
        assertThat(facade.getAxes()).hasSize(1);
    }

    // ─── 예외 ────────────────────────────────────────────────────

    @Test
    @DisplayName("addAxis_facade없음_예외 — LEARNING_FACADE_NOT_FOUND")
    void addAxis_facade없음_예외() {
        when(facadeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addAxis(new LearningFacadeCommand.AddAxis(1L, "Java 심화")))
                .isInstanceOf(com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode.LEARNING_FACADE_NOT_FOUND);
    }

    @Test
    @DisplayName("addAxis_axisId_null_예외 — JPA 회귀 시 IllegalStateException으로 즉시 실패")
    void addAxis_axisId_null_예외() {
        // save 후에도 id를 주입하지 않는 mock — JPA cascade 회귀 시나리오 모사
        when(facadeRepository.save(any(LearningFacade.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.addAxis(new LearningFacadeCommand.AddAxis(1L, "Java 심화")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LearningAxis");
    }
}
