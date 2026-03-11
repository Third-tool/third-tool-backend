package com.example.thirdtool.Card.domain.model;


import jakarta.persistence.*;
import org.springframework.data.annotation.Id;

import java.math.BigInteger;
import java.util.List;

@Entity
@Table(name = "card")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private BigInteger id;

    @Embedded
    private MainNote mainNote;

    @Embedded
    private Summary summary;


    protected Card() {}

    private Card(MainNote mainNote, Summary summary) {
        this.mainNote = mainNote;
        this.summary  = summary;
    }

    public static Card create(MainNote mainNote, Summary summary, List<String> keywordValues) {
        validateNotNull(mainNote,      "MainNote");
        validateNotNull(summary,       "Summary");
        validateNotNull(keywordValues, "KeywordCues");

        if (keywordValues.isEmpty()) {
            throw new CardDomainException("카드 생성 시 키워드는 최소 1개 이상이어야 합니다.");
        }

        Card card = new Card(mainNote, summary);
        keywordValues.forEach(v -> card.keywordCues.add(KeywordCue.create(card, v)));
        return card;
    }

    // -------------------------------------------------------------------------
    // 수정
    // -------------------------------------------------------------------------

    public void changeMainNote(String textContent, String imageUrl) {
        this.mainNote = MainNote.of(textContent, imageUrl);
    }

    public void changeSummary(String value) {
        this.summary = Summary.of(value);
    }

}
