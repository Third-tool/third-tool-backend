package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;

import java.util.Collections;
import java.util.List;

public class RelatedCardCandidate {

    private final Card       card;
    private final List<Tag> sharedTags;
    private final int        sharedTagCount;

    private RelatedCardCandidate(Card card, List<Tag> sharedTags) {
        this.card           = card;
        this.sharedTags     = Collections.unmodifiableList(sharedTags);
        this.sharedTagCount = sharedTags.size();
    }

    /**
     * CardRelationFinder 내부에서만 호출한다.
     * 외부에서 직접 생성하지 않는다.
     */
    static RelatedCardCandidate of(Card card, List<Tag> sharedTags) {
        if (card == null) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "RelatedCardCandidate: card는 null일 수 없습니다."
                                        );
        }
        if (sharedTags == null || sharedTags.isEmpty()) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "RelatedCardCandidate: sharedTags는 최소 1개 이상이어야 합니다."
                                        );
        }
        return new RelatedCardCandidate(card, sharedTags);
    }

    public Card      getCard()           { return card; }
    public List<Tag> getSharedTags()     { return sharedTags; }
    public int       getSharedTagCount() { return sharedTagCount; }
}