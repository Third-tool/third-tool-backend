package com.example.thirdtool.Library.domain.dto;

import java.time.LocalDateTime;

// library/api/dto/LibraryNameOnlyResponse.java
public record LibraryNameOnlyResponse(
        Long libraryEntryId,
        Long deckId,
        String deckName,
        String ownerName,
        LocalDateTime publishedAt
) {}
