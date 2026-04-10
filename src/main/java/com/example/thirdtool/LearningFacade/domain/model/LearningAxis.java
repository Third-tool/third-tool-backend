package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "learning_axis",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_learning_axis_facade_name",
                columnNames = {"learning_facade_id", "name"}
        )
)
public class LearningAxis {

    // ─── 식별자 ───────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_axis_id")
    private Long id;

    // ─── 소속 Facade ──────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_facade_id", nullable = false, updatable = false)
    private LearningFacade facade;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    // ─── 행동 목록 ────────────────────────────────────────
    // 축 삭제 시 orphanRemoval로 함께 삭제된다.
    // AxisAction은 Epic 2 Story 2-2에서 추가 예정
    @OneToMany(
            mappedBy      = "axis",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    private final List<AxisAction> actions = new ArrayList<>();

    // ─── 시각 ─────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** create() 내부 전용 생성자. */
    private LearningAxis(LearningFacade facade, String name, int displayOrder) {
        this.facade       = facade;
        this.name         = name;
        this.displayOrder = displayOrder;
    }

    // ─── 생성 ─────────────────────────────────────────────

    static LearningAxis create(LearningFacade facade, String name, int displayOrder) {
        requireNonNull(facade, "facade");
        validateName(name);
        if (displayOrder < 0) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 0 이상이어야 합니다. displayOrder=" + displayOrder
                                                  );
        }
        return new LearningAxis(facade, name.trim(), displayOrder);
    }

    // ─── 행위 ─────────────────────────────────────────────

    public void updateName(String newName) {
        validateName(newName);
        this.name = newName.trim();
    }

    void updateDisplayOrder(int newOrder) {
        if (newOrder < 0) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 0 이상이어야 합니다. newOrder=" + newOrder
                                                  );
        }
        this.displayOrder = newOrder;
    }

    // ─── 조회 ─────────────────────────────────────────────

    public List<AxisAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    // ─── 내부 검증 ────────────────────────────────────────

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_NAME_BLANK);
        }
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    fieldName + "은(는) null일 수 없습니다."
                                                  );
        }
    }
}