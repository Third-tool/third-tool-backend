package com.example.thirdtool.Card.presentation.dto.request;

import java.util.List;

    public record MoveCardsRequest(List<Long> cardIds, Long toDeckId) {}
