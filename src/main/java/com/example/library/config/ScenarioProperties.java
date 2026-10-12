package com.example.library.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 场景配置：self-study / reading 两个场景。
 * 以后加方向只加 yml，不改代码。
 */
@Data
@Component
@ConfigurationProperties(prefix = "library.scenario")
public class ScenarioProperties {

    private List<Scenario> items;

    @Data
    public static class Scenario {
        private String code;
        private String name;
        private int questionCount;
        private String reviewType;       // FREE_PRACTICE / RETELL / QUIZ
        private List<Long> bookCategoryIds;
        private int maxPoints;
    }
}
