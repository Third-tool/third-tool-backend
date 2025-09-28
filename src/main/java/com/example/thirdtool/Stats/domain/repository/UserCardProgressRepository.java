package com.example.thirdtool.Stats.domain.repository;

import com.example.thirdtool.Deck.presentation.dto.DeckSearchDto;
import com.example.thirdtool.Stats.domain.model.UserCardEntity;
import com.example.thirdtool.Stats.presentation.dto.TagStudyCountDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCardProgressRepository extends JpaRepository<UserCardEntity,Long> {

    @Query("SELECT new com.example.thirdtool.Stats.presentation.dto.TagStudyCountDto(t.nameKey, COUNT(uce)) " +
        "FROM UserCardEntity uce " +
        "JOIN uce.card c " +
        "JOIN c.deck d " +
        "JOIN d.tags t " +
        "WHERE uce.user.id = :userId AND uce.lastStudy >= :since " +
        "GROUP BY t.id, t.nameKey " +
        "ORDER BY COUNT(uce) DESC")
    List<TagStudyCountDto> findTagStudyCounts(@Param("userId")Long userId, @Param("since")LocalDateTime startOfMonth);

}
