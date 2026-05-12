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

    @Column(name = "author", length = 100)
    private String author;

    @Column(name = "platform", length = 100)
    private String platform;

    @Column(name = "ai_provider", length = 50)
    private String aiProvider;

    @Column(name = "web_source", length = 50)
    private String webSource;

    @Column(name = "memo", length = 1000)
    private String memo;

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
                             String url,
                             String author,
                             String platform,
                             String aiProvider,
                             String webSource,
                             String memo) {
        this.facade           = facade;
        this.name             = name;
        this.materialType     = materialType;
        this.url              = url;
        this.author           = author;
        this.platform         = platform;
        this.aiProvider       = aiProvider;
        this.webSource        = webSource;
        this.memo             = memo;
        this.proficiencyLevel = ProficiencyLevel.UNRATED;
    }

    public static LearningMaterial create(LearningFacade facade,
                                          String name,
                                          MaterialType materialType,
                                          String url,
                                          String author,
                                          String platform,
                                          String aiProvider,
                                          String webSource,
                                          String memo) {
        requireNonNull(facade, "facade");
        validateName(name);
        if (materialType == null) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_TYPE_REQUIRED);
        }
        return new LearningMaterial(
                facade,
                name.trim(),
                materialType,
                normalizeOptional(url),
                normalizeOptional(author),
                normalizeOptional(platform),
                normalizeOptional(aiProvider),
                normalizeOptional(webSource),
                normalizeOptional(memo)
        );
    }

    // url·부가 속성 미입력 케이스의 등록 편의 팩토리 (기존 호출자 호환)
    public static LearningMaterial create(LearningFacade facade,
                                          String name,
                                          MaterialType materialType,
                                          String url) {
        return create(facade, name, materialType, url, null, null, null, null, null);
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

    // 선택적 String 필드: trim 후 빈 문자열이면 null로 정규화 (domain-conventions §1)
    private static String normalizeOptional(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
