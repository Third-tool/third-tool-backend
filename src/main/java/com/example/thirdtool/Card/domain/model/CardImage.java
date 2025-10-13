package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "card_images")
public class CardImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false)
    private String imageUrl; // S3 URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImageType imageType; // QUESTION / ANSWER 구분

    @Column(nullable = false)
    private Integer sequence; // 여러 장일 경우 순서

    @Builder
    private CardImage(Card card, String imageUrl, ImageType imageType, Integer sequence) {
        this.card = card;
        this.imageUrl = imageUrl;
        this.imageType = imageType;
        this.sequence = sequence;
    }

    public static CardImage of(Card card, String imageUrl, ImageType imageType, Integer sequence) {
        return CardImage.builder()
                        .card(card)
                        .imageUrl(imageUrl)
                        .imageType(imageType)
                        .sequence(sequence)
                        .build();
    }

    public void updateImage(String newUrl, Integer newSequence) {
        this.imageUrl = newUrl;
        this.sequence = newSequence;
    }
}
