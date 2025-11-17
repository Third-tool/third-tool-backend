package com.example.thirdtool.Common.Util;


import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.model.ImageType;
import com.example.thirdtool.Deck.domain.model.Deck;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 덱 단위 대표 썸네일 정책:
 * 1) QUESTION 우선, 2) sequence ASC, 3) card.updatedAt DESC, 4) (optional) card.id ASC, 5) image.id ASC
 * - soft delete 된 카드는 제외
 * - URL 유효성 검사
 * - 결과 없으면 null 반환 (FE에서 이모티콘/기본 이미지로 처리)
 */
public final class DeckThumbnailPolicy {

    // 필요 시 공용 플레이스홀더 (현재 요구사항은 null 반환 → FE에서 이모티콘 처리)
    public static final String PLACEHOLDER = null;
    // public static final String PLACEHOLDER = "https://.../no-image.jpg";

    private DeckThumbnailPolicy() {}

    /** 덱 엔티티 내 메모리 객체만으로 썸네일 선택 (간단/소규모 덱에 적합) */
    public static String pick(Deck deck) {
        if (deck == null || deck.getCards() == null || deck.getCards().isEmpty()) {
            return PLACEHOLDER;
        }
        // 덱의 모든 CardImage를 평탄화
        List<CardImage> images = deck.getCards().stream()
                                     .filter(Objects::nonNull)
                                     .filter(card -> !isDeleted(card))
                                     .flatMap(card -> safeImages(card))
                                     .filter(Objects::nonNull)
                                     .toList();

        return pickFromImages(images);
    }

    /** 카드 컬렉션으로부터 선택 (덱이 아닌 하위 계층에서 바로 호출하고 싶을 때) */
    public static String pickFromCards(Collection<Card> cards) {
        if (cards == null || cards.isEmpty()) return PLACEHOLDER;

        List<CardImage> images = cards.stream()
                                      .filter(Objects::nonNull)
                                      .filter(card -> !isDeleted(card))
                                      .flatMap(DeckThumbnailPolicy::safeImages)
                                      .filter(Objects::nonNull)
                                      .toList();

        return pickFromImages(images);
    }

    /** 이미지 컬렉션에서 직접 선택 (이미 리포지토리 등에서 필터링해 가져온 경우) */
    public static String pickFromImages(Collection<CardImage> images) {
        if (images == null || images.isEmpty()) return PLACEHOLDER;

        return images.stream()
                     .filter(Objects::nonNull)
                     .filter(img -> isUsableUrl(img.getImageUrl()))
                     .sorted(DeckThumbnailPolicy::compareByPolicy)
                     .map(CardImage::getImageUrl)
                     .findFirst()
                     .orElse(PLACEHOLDER);
    }

    /**
     * (옵션) 대용량 덱/성능 고려 버전:
     * 덱 ID만 받아서, 외부에서 효율적으로 조회해주는 로더(예: JPQL 1건 조회)를 주입.
     * 예) loader = id -> cardImageRepository.pickDeckThumbnailTopK(id, 8);
     */
    public static String pickByLoader(Long deckId, Function<Long, Collection<CardImage>> loader) {
        if (deckId == null || loader == null) return PLACEHOLDER;
        Collection<CardImage> images = loader.apply(deckId);
        return pickFromImages(images);
    }

    /* ---------------------- 내부 유틸 ---------------------- */

    private static Stream<CardImage> safeImages(Card card) {
        if (card.getImages() == null || card.getImages().isEmpty()) return Stream.empty();
        return card.getImages().stream();
    }

    private static boolean isDeleted(Card card) {
        // soft delete 플래그가 true면 제외
        try {
            return card.isDeleted();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isUsableUrl(String s) {
        if (s == null) return false;
        String x = s.trim();
        if (x.isEmpty()) return false;
        if ("about:blank".equalsIgnoreCase(x)) return false;
        return true;
    }

    private static int compareByPolicy(CardImage a, CardImage b) {
        // 1) 타입 우선순위
        int ta = typeRank(a.getImageType());
        int tb = typeRank(b.getImageType());
        if (ta != tb) return Integer.compare(ta, tb);

        // 2) 시퀀스 ASC (nulls last)
        Integer sa = a.getSequence();
        Integer sb = b.getSequence();
        if (!Objects.equals(sa, sb)) {
            if (sa == null) return 1;    // nulls last
            if (sb == null) return -1;
            int cmp = Integer.compare(sa, sb);
            if (cmp != 0) return cmp;
        }

        // 4) card.id ASC (tie-break)
        Long ca = a.getCard() != null ? a.getCard().getId() : null;
        Long cb = b.getCard() != null ? b.getCard().getId() : null;
        if (!Objects.equals(ca, cb)) {
            if (ca == null) return 1;
            if (cb == null) return -1;
            int cmp = Long.compare(ca, cb);
            if (cmp != 0) return cmp;
        }

        // 5) image.id ASC (tie-break)
        Long ia = a.getId();
        Long ib = b.getId();
        if (!Objects.equals(ia, ib)) {
            if (ia == null) return 1;
            if (ib == null) return -1;
            return Long.compare(ia, ib);
        }
        return 0;
    }

    private static int typeRank(ImageType t) {
        if (t == ImageType.QUESTION) return 0;
        if (t == ImageType.ANSWER)   return 1;
        return 2; // 혹시 모를 확장 대비
    }
}
