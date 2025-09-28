package com.example.thirdtool.Stats.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.User.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    Card card;

    @Column(nullable = false)
    private int correctCount;
    @Column(nullable = false)
    private int incorrectCount;
    @Column(nullable = false)
    private int studyCount;
    @Column(nullable = false)
    private LocalDateTime lastStudy;

    private UserCardEntity(User user, Card card) {
        this.user = user;
        this.card = card;
        this.lastStudy = LocalDateTime.now();
    }

    public static UserCardEntity of(User user, Card card) {
        return new UserCardEntity(user, card);
    }

    public void recordAnswer(boolean answer){
        if(answer){
            this.correctCount++;
        }
        else {
            this.incorrectCount++;
        }
        this.lastStudy = LocalDateTime.now();
    }
}
