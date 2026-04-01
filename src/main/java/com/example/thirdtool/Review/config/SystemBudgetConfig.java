package com.example.thirdtool.Review.config;

import com.example.thirdtool.Card.domain.model.OnFieldBudget;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class SystemBudgetConfig {

    @Value("${thirdtool.budget.max-view:10}")
    private int maxView;

    @Value("${thirdtool.budget.max-duration-days:30}")
    private int maxDurationDays;

    @Bean
    public OnFieldBudget systemOnFieldBudget() {
        return OnFieldBudget.of(maxView, Duration.ofDays(maxDurationDays));
    }
}
