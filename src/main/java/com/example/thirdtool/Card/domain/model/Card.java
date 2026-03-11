package com.example.thirdtool.Card.domain.model;


import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.persistence.*;
import org.springframework.data.annotation.Id;

import java.math.BigInteger;
import java.util.Collections;
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
        requireNonNull(mainNote,      "mainNote");
        requireNonNull(summary,       "summary");
        requireNonNull(keywordValues, "keywordValues");

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

    public void replaceKeywords(List<String> values) {
        requireNonNull(values, "values");
        if (values.isEmpty()) {
            throw CardDomainException.of(ErrorCode.CARD_KEYWORD_MIN_REQUIRED);
        }
        this.keywordCues.clear();
        values.forEach(v -> this.keywordCues.add(KeywordCue.create(this, v)));
    }

    public void addKeyword(String value) {
        this.keywordCues.add(KeywordCue.create(this, value));
    }


    public void removeKeyword(Long keywordCueId) {
        KeywordCue target = keywordCues.stream()
                                       .filter(c -> keywordCueId.equals(c.getId()))
                                       .findFirst()
                                       .orElseThrow(() -> CardDomainException.of(
                                               ErrorCode.CARD_KEYWORD_NOT_FOUND,
                                               "keywordCueId=" + keywordCueId));

        if (keywordCues.size() <= 1) {
            throw CardDomainException.of(ErrorCode.CARD_KEYWORD_LAST_CANNOT_REMOVE);
        }
        keywordCues.remove(target);
    }

    public Long             getId()          { return id; }
    public MainNote         getMainNote()    { return mainNote; }
    public Summary          getSummary()     { return summary; }

    /** 수정 불가능한 뷰를 반환한다. Aggregate 외부에서 직접 컬렉션 조작 불가. */
    public List<KeywordCue> getKeywordCues() {
        return Collections.unmodifiableList(keywordCues);
    }


    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw CardDomainException.of(ErrorCode.INVALID_INPUT, fieldName + "은(는) null일 수 없습니다.");
        }
    }

}
