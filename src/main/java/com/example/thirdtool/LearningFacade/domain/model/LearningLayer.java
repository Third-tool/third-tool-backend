package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * LearningFacade 하위, LearningAxis 상위 그룹핑 계층 (Story-LT-E2-S1).
 *
 * <p>사용자 자산성 도메인 → Soft Delete 적용 ({@code @SQLRestriction} + {@code @SQLDelete}).
 * default Layer 이름 "Uncategorized"는 {@link LearningFacade#create}에서 자동 생성되며,
 * 마이그레이션 V19의 backfill이 기존 axis를 이 layer로 이관한다.
 *
 * <p>{@code softDelete()}는 활성 axis가 존재하면 {@link ErrorCode#LEARNING_LAYER_HAS_ACTIVE_AXES}를 던진다
 * — 자동 연쇄 삭제 대신 사용자에게 명시적 이동/삭제를 요구 (Epic 2 기술 결정).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "learning_layer",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_learning_layer_facade_name_deleted",
                columnNames = {"learning_facade_id", "name", "deleted_at"}
        )
)
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE learning_layer SET deleted_at = CURRENT_TIMESTAMP(6) WHERE learning_layer_id = ?")
public class LearningLayer {

    /** default Layer 이름 — 마이그레이션·자동생성·조회에 재사용. */
    public static final String DEFAULT_LAYER_NAME = "Uncategorized";

    public static final int MAX_LAYER_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_layer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_facade_id", nullable = false, updatable = false)
    private LearningFacade facade;

    @Column(name = "name", nullable = false, length = MAX_LAYER_NAME_LENGTH)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /**
     * Layer 소속 활성 axis 목록.
     * {@link LearningAxis} 도메인이 개별 {@code @SQLRestriction("deleted_at IS NULL")}을 갖고 있어
     * 조회 시점에 자동 필터된다.
     */
    @OneToMany(
            mappedBy      = "layer",
            cascade       = CascadeType.ALL,
            orphanRemoval = false,
            fetch         = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC")
    private final List<LearningAxis> axes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private LearningLayer(LearningFacade facade, String name, int displayOrder) {
        this.facade       = facade;
        this.name         = name;
        this.displayOrder = displayOrder;
    }

    /**
     * 정적 팩토리. Aggregate Root({@link LearningFacade})의 addLayer 경로에서만 호출된다.
     * name은 trim + blank 거부 + 길이 검증. displayOrder는 1-based.
     */
    static LearningLayer of(LearningFacade facade, String name, int displayOrder) {
        if (facade == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "facade는 null일 수 없습니다."
            );
        }
        String normalized = normalizeName(name);
        if (displayOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. displayOrder=" + displayOrder
            );
        }
        return new LearningLayer(facade, normalized, displayOrder);
    }

    static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_LAYER_NAME_BLANK);
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_LAYER_NAME_LENGTH) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_LAYER_NAME_TOO_LONG,
                    "length=" + trimmed.length()
            );
        }
        return trimmed;
    }

    void updateDisplayOrder(int newOrder) {
        if (newOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. newOrder=" + newOrder
            );
        }
        this.displayOrder = newOrder;
    }

    // ─── 소프트 삭제 ─────────────────────────────────────

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Layer softDelete (Story-LT-E2-S4).
     *
     * <p>정책:
     * <ul>
     *   <li>이미 삭제 상태에서 재호출 → {@link ErrorCode#LEARNING_LAYER_ALREADY_DELETED}</li>
     *   <li>활성 자식 axis가 존재 → {@link ErrorCode#LEARNING_LAYER_HAS_ACTIVE_AXES} (자동 연쇄 X)</li>
     *   <li>실제 반영은 {@code @SQLDelete}가 UPDATE로 처리 — 여기서는 in-memory 상태만 갱신</li>
     * </ul>
     */
    public void softDelete() {
        if (isDeleted()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_LAYER_ALREADY_DELETED);
        }
        if (axes.stream().anyMatch(a -> !a.isDeleted())) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_LAYER_HAS_ACTIVE_AXES);
        }
        this.deletedAt = LocalDateTime.now();
    }

    /** default Uncategorized Layer 여부. */
    public boolean isDefault() {
        return DEFAULT_LAYER_NAME.equals(this.name);
    }

    // ─── name 갱신 ───────────────────────────────────────

    /**
     * Layer 이름 변경 (Story-LT-E2-S5).
     * 동일 값(trim 후) 입력이면 no-op.
     * Facade 내 다른 활성 Layer와 중복 여부는 {@link LearningFacade#validateLayerNameDuplicate}가 검증.
     *
     * @return 실제 변경이 발생했는지 여부
     */
    boolean updateName(String newName) {
        String normalized = normalizeName(newName);
        if (this.name.equals(normalized)) {
            return false;
        }
        this.name = normalized;
        return true;
    }

    // ─── axis 목록 관리 ───────────────────────────────────

    /**
     * Layer 소속 axis 추가. 이름 정규화·displayOrder 부여·중복 검증은 Facade에서 사전 확인 후 호출한다.
     * displayOrder는 이 Layer 내부의 활성 axis 중 max + 1 (전역 아님).
     */
    LearningAxis addAxis(String name) {
        int nextOrder = (int) axes.stream().filter(a -> !a.isDeleted()).count() + 1;
        LearningAxis axis = LearningAxis.createInLayer(this.facade, this, name, nextOrder);
        axes.add(axis);
        return axis;
    }

    /** Layer 내 활성 axis만 반환 (읽기 편의). */
    public List<LearningAxis> getAxes() {
        return axes.stream()
                   .filter(a -> !a.isDeleted())
                   .toList();
    }

    /**
     * Layer 내 axis 순서 재배치. 전달 id 목록이 이 layer의 axis id 집합과 정확히 일치해야 함.
     */
    void reorderAxes(List<Long> orderedAxisIds) {
        if (orderedAxisIds == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "orderedAxisIds는 null일 수 없습니다."
            );
        }
        Set<Long> currentIds = getAxes().stream()
                                        .map(LearningAxis::getId)
                                        .collect(Collectors.toSet());
        Set<Long> incoming = new HashSet<>(orderedAxisIds);
        if (currentIds.size() != orderedAxisIds.size() || !currentIds.equals(incoming)) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_AXIS_REORDER_MISMATCH,
                    "layer id=" + this.id
            );
        }
        IntStream.range(0, orderedAxisIds.size()).forEach(i -> {
            Long id = orderedAxisIds.get(i);
            LearningAxis target = axes.stream()
                    .filter(a -> id.equals(a.getId()))
                    .findFirst()
                    .orElseThrow();
            target.updateDisplayOrder(i + 1);
        });
    }
}
