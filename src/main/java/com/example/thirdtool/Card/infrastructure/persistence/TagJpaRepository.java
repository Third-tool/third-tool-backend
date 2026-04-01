package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagJpaRepository extends JpaRepository<Tag, Long> {

    /**
     * 태그 이름으로 단건 조회.
     * Tag.value에 DB 유니크 제약이 걸려 있으므로 0 또는 1건이 반환된다.
     */
    Optional<Tag> findByValue(String value);
}
