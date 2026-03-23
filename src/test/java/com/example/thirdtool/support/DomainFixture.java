package com.example.thirdtool.support;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.domain.model.SocialProviderType;
import com.example.thirdtool.User.domain.model.UserEntity;

import java.util.List;


public class DomainFixture {

    // ─── UserEntity ───────────────────────────────────────

    /**
     * 기본 로컬 유저 (자체 로그인)
     * username / encodedPassword / nickname / email 기본값 사용
     */
    public static UserEntity sampleLocalUser() {
        return UserEntity.ofLocal(
                "testuser",
                "encoded_password",
                "테스트유저",
                "testuser@example.com"
                                 );
    }

    /**
     * 로컬 유저 — username / nickname 지정
     */
    public static UserEntity sampleLocalUser(String username, String nickname) {
        return UserEntity.ofLocal(
                username,
                "encoded_password",
                nickname,
                username + "@example.com"
                                 );
    }

    /**
     * 카카오 소셜 유저
     */
    public static UserEntity sampleKakaoUser() {
        return UserEntity.ofSocial(
                "kakao_testuser",
                SocialProviderType.KAKAO,
                "카카오유저",
                "kakao@example.com"
                                  );
    }

    /**
     * 네이버 소셜 유저
     */
    public static UserEntity sampleNaverUser() {
        return UserEntity.ofSocial(
                "naver_testuser",
                SocialProviderType.NAVER,
                "네이버유저",
                "naver@example.com"
                                  );
    }

    // ─── Deck ─────────────────────────────────────────────

    /**
     * 기본 루트 덱 (parentDeck = null, depth = 0)
     * 내부적으로 sampleLocalUser() 사용
     */
    public static Deck sampleDeck() {
        return Deck.of("테스트덱", null, sampleLocalUser());
    }

    /**
     * 덱 이름 지정 루트 덱
     */
    public static Deck sampleDeck(String name) {
        return Deck.of(name, null, sampleLocalUser());
    }

    /**
     * 유저 지정 루트 덱
     */
    public static Deck sampleDeck(String name, UserEntity user) {
        return Deck.of(name, null, user);
    }

    /**
     * 하위 덱 — 부모 덱 지정 (depth 자동 계산)
     * 유저는 부모 덱의 유저를 그대로 사용
     */
    public static Deck sampleSubDeck(String name, Deck parentDeck) {
        return Deck.of(name, parentDeck, parentDeck.getUser());
    }

    // ─── Card ─────────────────────────────────────────────

    /**
     * 기본 카드 (텍스트 MainNote + Summary + 키워드 3개)
     */
    public static Card sampleCard() {
        return Card.create(
                sampleDeck(),
                MainNote.of("스택은 LIFO 자료구조다.", null),
                Summary.of("스택은 마지막에 넣은 것이 먼저 나온다."),
                List.of("LIFO란?", "push란?", "pop이란?")
                          );
    }

    /**
     * 덱 지정 카드
     */
    public static Card sampleCard(Deck deck) {
        return Card.create(
                deck,
                MainNote.of("스택은 LIFO 자료구조다.", null),
                Summary.of("스택은 마지막에 넣은 것이 먼저 나온다."),
                List.of("LIFO란?", "push란?", "pop이란?")
                          );
    }

    /**
     * 내용 전체 지정 카드
     */
    public static Card sampleCard(Deck deck,
                                  String mainText,
                                  String summaryText,
                                  List<String> keywords) {
        return Card.create(
                deck,
                MainNote.of(mainText, null),
                Summary.of(summaryText),
                keywords
                          );
    }
}