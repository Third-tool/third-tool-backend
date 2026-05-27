package com.example.thirdtool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableJpaAuditing
@EnableScheduling
@ConfigurationPropertiesScan("com.example.thirdtool")
@SpringBootApplication(scanBasePackages = "com.example.thirdtool")
public class ThirdToolApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThirdToolApplication.class, args);
    }

}
