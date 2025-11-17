package com.example.thirdtool.Common.Util;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.model.ImageType;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public final class ThumbnailPolicy {

    // 필요하면 공용 기본 이미지(플레이스홀더) 지정
    public static final String PLACEHOLDER =
            "https://third-tool-s3-server.s3.ap-northeast-2.amazonaws.com/common/no-image.jpg";

    private ThumbnailPolicy() {}

    /** 카드의 대표 썸네일 URL을 내부 정책에 따라 선택한다. */
    public static String pick(Card card) {
        if (card == null || card.getImages() == null || card.getImages().isEmpty()) {
            return null; // 혹은 PLACEHOLDER
        }

        // 1) QUESTION 타입 중 sequence 가장 작은 것
        Optional<String> q = firstUrlByType(card, ImageType.QUESTION);
        if (q.isPresent()) return q.get();

        // 2) ANSWER 타입 중 sequence 가장 작은 것
        Optional<String> a = firstUrlByType(card, ImageType.ANSWER);
        if (a.isPresent()) return a.get();

        // 3) 타입 무관, sequence 가장 작은 것
        Optional<String> any = firstUrlAny(card);
        return any.orElse(null); // 혹은 PLACEHOLDER
    }

    private static Optional<String> firstUrlByType(Card card, ImageType type) {
        return card.getImages().stream()
                   .filter(Objects::nonNull)
                   .filter(img -> img.getImageType() == type)
                   .sorted(Comparator.comparing(CardImage::getSequence, Comparator.nullsLast(Integer::compareTo)))
                   .map(CardImage::getImageUrl)
                   .filter(ThumbnailPolicy::isUsableUrl)
                   .findFirst();
    }

    private static Optional<String> firstUrlAny(Card card) {
        return card.getImages().stream()
                   .filter(Objects::nonNull)
                   .sorted(Comparator.comparing(CardImage::getSequence, Comparator.nullsLast(Integer::compareTo)))
                   .map(CardImage::getImageUrl)
                   .filter(ThumbnailPolicy::isUsableUrl)
                   .findFirst();
    }

    private static boolean isUsableUrl(String s) {
        if (s == null) return false;
        String x = s.trim();
        if (x.isEmpty()) return false;
        if ("about:blank".equalsIgnoreCase(x)) return false;
        return true;
    }
}
