package com.example.thirdtool.Library.domain;

import com.example.thirdtool.Common.BaseEntity;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// library/domain/LibraryEntry.java
@Entity
@Table(name = "library_entry",
       uniqueConstraints = @UniqueConstraint(name = "uk_library_entry_deck", columnNames = "deck_id"),
       indexes = @Index(name = "idx_library_entry_published_at", columnList = "published_at DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LibraryEntry extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 누가 올렸는지 (표시용)
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    // 어떤 덱에서 온 건지(실제 조회/복제는 안 함, 레퍼런스만 보관)
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    // 🔥 이번 단계 핵심: 이름 스냅샷만 저장
    @Column(name = "deck_name_snapshot", nullable = false, length = 100)
    private String deckNameSnapshot;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "public_visible", nullable = false)
    private boolean publicVisible = true;

    @Builder
    private LibraryEntry(UserEntity owner, Deck deck, String deckNameSnapshot) {
        this.owner = owner;
        this.deck = deck;
        this.deckNameSnapshot = deckNameSnapshot;
        this.publishedAt = LocalDateTime.now();
        this.publicVisible = true;
    }

    public static LibraryEntry of(Deck deck) {
        return LibraryEntry.builder()
                           .owner(deck.getUser())
                           .deck(deck)
                           .deckNameSnapshot(deck.getName())
                           .build();
    }
}
