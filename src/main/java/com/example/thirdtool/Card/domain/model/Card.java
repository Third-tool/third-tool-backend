package com.example.thirdtool.Card.domain.model;


import jakarta.persistence.*;
import org.springframework.data.annotation.Id;

import java.math.BigInteger;

@Entity
@Table(name = "card")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private BigInteger id;

    @Embedded
    private MainNote mainNote;

    @Embedded
    private Summary summary;

}
