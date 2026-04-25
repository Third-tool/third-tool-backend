package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "learning_material")
public class LearningMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_material_id")
    private Long id;

    // ─── 소속 Facade ──────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_facade_id", nullable = false, updatable = false)
    private LearningFacade facade;

    // ─── 자료 정보 ────────────────────────────────────────
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_type", nullable = false, length = 20, updatable = false)
    private MaterialType materialType;

    // nullable. v1에서는 입력을 권장하지만 강제하지 않는다.
    @Column(name = "url", length = 500)
    private String url;

    // ─── 숙련도 ───────────────────────────────────────────
    // 생성 시 반드시 UNRATED로 초기화된다. 외부 주입 불가.
    // 슬라이더를 한 번이라도 조작하면 UNRATED로 돌아올 수 없다. (Application Service 강제)
    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", nullable = false, length = 20)
    private ProficiencyLevel proficiencyLevel;

    // ─── 행동 매핑 목록 ───────────────────────────────────
    // LearningMaterial 삭제 시 orphanRemoval로 ActionMaterial이 함께 삭제된다.
    // 삭제 후 Application Service가 영향받은 행동들의 커버리지를 재계산한다.
    @OneToMany(
            mappedBy      = "material",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    private final List<ActionMaterial> actionMappings = new ArrayList<>();

    // ─── 시각 ─────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private LearningMaterial(LearningFacade facade,
                             String name,
                             MaterialType materialType,
                             String url) {
        this.facade           = facade;
        this.name             = name;
        this.materialType     = materialType;
        this.url              = url;
        this.proficiencyLevel = ProficiencyLevel.UNRATED; // 항상 UNRATED로 초기화
    }

    // ─── 생성 ─────────────────────────────────────────────

    /**
     * 학습 자료를 생성한다.
     *
     * <p>{@code proficiencyLevel}은 {@link ProficiencyLevel#UNRATED}로 자동 초기화된다.
     * 외부에서 초기 숙련도를 지정할 수 없다.
     *
     * @param facade       소속 LearningFacade. null 불가.
     * @param name         자료명. null·blank 불가.
     * @param materialType 자료 유형 (TOP_DOWN / BOTTOM_UP). null 불가.
     * @param url          외부 자료 링크. nullable.
     */
    public static LearningMaterial create(LearningFacade facade,
                                          String name,
                                          MaterialType materialType,
                                          String url) {
        requireNonNull(facade, "facade");
        validateName(name);
        requireNonNull(materialType, "materialType");
        return new LearningMaterial(facade, name.trim(), materialType, url);
    }

    // ─── 행위 ─────────────────────────────────────────────

    /**
     * 자료명을 변경한다.
     *
     * @param newName 새 자료명. null·blank 불가.
     */
    public void updateName(String newName) {
        validateName(newName);
        this.name = newName.trim();
    }

    /**
     * 숙련도 자가 평가를 변경한다.
     *
     * <p>Application Service가 이 메서드 호출 후 연결된 행동들의 커버리지를 재계산한다.
     *
     * <p>UNRATED로의 복귀 차단은 Application Service 책임이다.
     * 도메인은 null 방어만 수행한다.
     *
     * @param level 변경할 숙련도. null 불가.
     */
    public void updateProficiencyLevel(ProficiencyLevel level) {
        requireNonNull(level, "proficiencyLevel");
        this.proficiencyLevel = level;
    }

    // ─── 조회 ─────────────────────────────────────────────

    public List<ActionMaterial> getActionMappings() {
        return Collections.unmodifiableList(actionMappings);
    }

    // ─── 내부 검증 ────────────────────────────────────────

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "자료명은 비어 있을 수 없습니다."
            );
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