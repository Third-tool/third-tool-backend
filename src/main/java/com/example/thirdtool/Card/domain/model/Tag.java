package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(
        name = "tag",
        uniqueConstraints = @UniqueConstraint(name = "uk_tag_value", columnNames = "value")
)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Column(name = "value", nullable = false, length = 50)
    private String value;

    // ─── 역방향 참조 ─────────────────────────────────────────
    // CardRelationFinder의 역방향 조회 및 태그별 카드 수 집계에 활용한다.
    @OneToMany(mappedBy = "tag", fetch = FetchType.LAZY)
    private final List<CardTag> cardTags = new ArrayList<>();

    /** JPA 전용 기본 생성자. 외부에서 직접 사용 금지. */
    protected Tag() {}

    private Tag(String value) {
        this.value = value;
    }

    /**
     * 태그를 생성한다.
     * value는 trim 처리 후 저장한다. - 전처리 과정
     * null 또는 blank이면 생성할 수 없다.
     */
    public static Tag of(String value) {
        String trimmed = trim(value);
        if (trimmed == null || trimmed.isBlank()) {
            throw CardDomainException.of(ErrorCode.TAG_VALUE_BLANK);
        }
        return new Tag(trimmed);
    }

    /**
     * 이 태그에 연결된 카드 수를 반환한다.
     * 태그 사용 통계 및 관련 카드 후보 조회에 활용한다.
     */
    public int getLinkedCardCount() {
        return cardTags.size();
    }

    /** 수정 불가능한 뷰를 반환한다. */
    public List<CardTag> getCardTags() {
        return Collections.unmodifiableList(cardTags);
    }

    private static String trim(String v) {
        return v == null ? null : v.trim();
    }
}