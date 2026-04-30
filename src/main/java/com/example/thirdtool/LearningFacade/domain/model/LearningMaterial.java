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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_facade_id", nullable = false, updatable = false)
    private LearningFacade facade;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_type", nullable = false, length = 20, updatable = false)
    private MaterialType materialType;

    @Column(name = "url", length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", nullable = false, length = 20)
    private ProficiencyLevel proficiencyLevel;

    @OneToMany(
            mappedBy      = "material",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    private final List<TopicMaterial> topicMappings = new ArrayList<>();

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
        this.proficiencyLevel = ProficiencyLevel.UNRATED;
    }

    public static LearningMaterial create(LearningFacade facade,
                                          String name,
                                          MaterialType materialType,
                                          String url) {
        requireNonNull(facade, "facade");
        validateName(name);
        if (materialType == null) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_TYPE_REQUIRED);
        }
        return new LearningMaterial(facade, name.trim(), materialType, url);
    }

    public void updateName(String newName) {
        validateName(newName);
        this.name = newName.trim();
    }

    public void updateProficiencyLevel(ProficiencyLevel level) {
        requireNonNull(level, "proficiencyLevel");
        this.proficiencyLevel = level;
    }

    public List<TopicMaterial> getTopicMappings() {
        return Collections.unmodifiableList(topicMappings);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_NAME_BLANK);
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
