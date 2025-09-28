package com.example.thirdtool.Stats.presentation.controller;

import com.example.thirdtool.Stats.application.service.StatsService;
import com.example.thirdtool.Stats.presentation.dto.TagStudyCountDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/monthly-tag-ratio")
    public ResponseEntity<List<TagStudyCountDto>> getMonthlyTagRatio() {
        Long userId = 1L; // TODO jwt 활용해서 주입
        List<TagStudyCountDto> monthlyStats = statsService.getThisMonthTagRatio(userId);
        return ResponseEntity.ok(monthlyStats);
    }

}
