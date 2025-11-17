package com.example.thirdtool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = "com.example.thirdtool")
@EnableElasticsearchRepositories(basePackages = "com.example.thirdtool.Card.domain.repository")
public class ThirdToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThirdToolApplication.class, args);
    }

}
