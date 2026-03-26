package com.example.thirdtool.Review.domain.model;

import com.example.thirdtool.Card.domain.model.KeywordCue;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;

import java.util.List;

public record CardVisibleContent(
        MainNote mainNote,           // 항상 포함
        List<KeywordCue> keywordCues,// RECALLING이면 null
        Summary summary,             // RECALLING이면 null
        ReviewStep reviewStep
) {

    /** RECALLING 단계 — Main만 노출. */
    static CardVisibleContent recalling(MainNote mainNote) {
        return new CardVisibleContent(mainNote, null, null, ReviewStep.RECALLING);
    }

    /** COMPARING 단계 — Main + Keywords + Summary 전체 노출. */
    static CardVisibleContent comparing(MainNote mainNote,
                                        List<KeywordCue> keywordCues,
                                        Summary summary) {
        return new CardVisibleContent(mainNote, keywordCues, summary, ReviewStep.COMPARING);
    }
}