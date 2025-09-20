package com.example.thirdtool.Tag.domain.model;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Builder
@Getter
@Setter
@Table(name = "tag", uniqueConstraints = {
    // ✅ "사용자 ID"와 "태그 이름"의 조합이 유니크하도록 제약조건 설정
    @UniqueConstraint(
        columnNames = {"user_id", "name_key"}
    )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max=64)
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "name_key", nullable = false, length = 64)
    private String nameKey;

    @ManyToMany(mappedBy = "tags") // ✅ 나는 연관관계의 주인이 아님을 명시
    private Set<Deck> decks = new HashSet<>();


    @PrePersist @PreUpdate
    private void normalize(){
        String src = (displayName == null) ? "" : displayName;
        String norm = Normalizer.normalize(src, Normalizer.Form.NFKC);
        norm = norm.strip();
        this.displayName = norm;
        this.nameKey = norm.toLowerCase();
    }

    private Tag(String newDisplayName) {
        this.displayName = newDisplayName;
    }

    public static Tag of(String name, User user) {
        validateName(name);
        Tag t = new Tag(name);
        t.user = user;
        return t;
    }

    public static void validateName(String name){ //이름 검증
        if (name == null || name.isEmpty()){
            throw new IllegalArgumentException("태그 이름은 비어있을 수 없습니다.");
        }
        if (name.length()>20){
            throw new IllegalArgumentException("태그의 길이는 20자를 넘을 수 없습니다");
        }
    }

    public void updateName(String newDisplayName) { //태그 이름 업데이트
        validateName(newDisplayName);
        this.displayName = newDisplayName;
    }

    public User getUser() {
        return user;
    }

}