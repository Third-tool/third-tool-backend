package com.example.thirdtool.Card.domain.model;


import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
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
            throw CardDomainException.of(ErrorCode.CARD_KEYWORD_MIN_REQUIRED);
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
