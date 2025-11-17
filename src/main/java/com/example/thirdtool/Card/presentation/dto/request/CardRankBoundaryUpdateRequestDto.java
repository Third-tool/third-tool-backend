package com.example.thirdtool.Card.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CardRankBoundaryUpdateRequestDto {
    private Integer silverGoldBoundary;   // SILVER의 max
    private Integer goldDiamondBoundary;  // GOLD의 max
    private Integer diamondMax;           // DIAMOND의 max (바의 끝값)
}

