package com.example.thirdtool.Card.domain.model;


import com.example.thirdtool.User.domain.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "card_ranks")
@Entity
public class CardRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name; // 예: "GOLD"

    @Column(nullable = false)
    private int minScore; // 최소 점수

    @Column(nullable = false)
    private int maxScore; // 최대 점수

    // ✅ CardRank가 특정 User에 속하게 됩니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ✅ 빌더를 위한 private 생성자
    @Builder(builderMethodName = "internalBuilder")
    private CardRank(String name, int minScore, int maxScore, User user) {
        this.name = name;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.user = user;
    }

    // ✅ 정적 팩토리 메서드
    public static CardRank createRank(String name, int minScore, int maxScore, User user) {
        return internalBuilder()
                .name(name)
                .minScore(minScore)
                .maxScore(maxScore)
                .user(user)
                .build();
    }

    // ✅ 점수 범위를 업데이트하는 메서드
    public void updateScoreRange(int minScore, int maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

}