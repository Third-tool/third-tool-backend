package com.example.thirdtool.Card.domain.model;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class MainNote {
    @Column(name = "main_text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "main_image_url", length = 2048)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "main_content_type", nullable = false)
    private MainContentType contentType;

    protected MainNote() {
    }

    private MainNote(String textContent, String imageUrl) {
        this.textContent = textContent;
        this.imageUrl    = imageUrl;
        this.contentType = MainContentType.resolve(hasText(textContent), hasImage(imageUrl));
    }

    /**
     * MainNote 생성 팩토리 메서드.
     *
     * @throws CardDomainException textContent와 imageUrl이 모두 비어있을 때
     */
    public static MainNote of(String textContent, String imageUrl) {
        String trimmedText  = trim(textContent);
        String trimmedImage = trim(imageUrl);

        if (!hasText(trimmedText) && !hasImage(trimmedImage)) {
            throw new CardDomainException("MainNote는 텍스트 또는 이미지 중 최소 하나를 포함해야 합니다.");
        }
        return new MainNote(trimmedText, trimmedImage);
    }



    public String          getTextContent() { return textContent; }
    public String          getImageUrl()    { return imageUrl; }
    public MainContentType getContentType() { return contentType; }

    private static String  trim(String v)     { return v == null ? null : v.trim(); }
    private static boolean hasText(String v)  { return v != null && !v.isBlank(); }
    private static boolean hasImage(String v) { return v != null && !v.isBlank(); }

}
