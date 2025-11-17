package com.example.thirdtool.Card.Document;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Common.Util.UUIDUtil;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "card_index")  // ✅ 인덱스 이름
public class CardDocument {

    @Id
    private String esId; // ✅ Elasticsearch 내부 식별자 (UUID 등)

    @Field(type = FieldType.Keyword)
    private Long cardId; // ✅ MySQL Card ID 저장

    @Field(type = FieldType.Keyword) //특정 user의 id여야 하니까 ㅇㅇ
    private Long userId;

    @Field(type = FieldType.Text, analyzer = "nori") // ✅ 한국어 분석기
    private String question;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String answer;

    @Field(type = FieldType.Keyword)
    private Long deckId;

    /** ✅ CardDocument 생성 팩토리 */
    public static CardDocument from(Card card) {
        if (card.getDeck() == null || card.getDeck().getUser() == null) {
            throw new IllegalStateException("Deck 또는 User 정보가 존재하지 않습니다.");
        }

        return CardDocument.builder()
                           .esId(UUIDUtil.generateV7())     // ES용 고유 식별자
                           .cardId(card.getId())
                           .deckId(card.getDeck().getId())
                           .userId(card.getDeck().getUser().getId())   // ✅ Deck을 통해 UserId 추출
                           .question(card.getQuestion())
                           .answer(card.getAnswer())
                           .build();
    }
}