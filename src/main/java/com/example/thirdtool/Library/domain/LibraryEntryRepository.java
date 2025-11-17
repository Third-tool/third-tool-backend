package com.example.thirdtool.Library.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// library/domain/LibraryEntryRepository.java
public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, Long> {
    Optional<LibraryEntry> findByDeckId(Long deckId);
    Page<LibraryEntry> findByPublicVisibleTrueOrderByPublishedAtDesc(Pageable pageable);
}
