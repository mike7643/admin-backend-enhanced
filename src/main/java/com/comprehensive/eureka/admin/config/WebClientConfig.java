package com.comprehensive.eureka.admin.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.base-url}")
    private String baseUrl;

    @Bean
    @Qualifier("chatbotClient")
    public WebClient chatbotClient(WebClient.Builder builder) {
        return builder
                .baseUrl(baseUrl)    // 플레이스홀더가 아닌 주입된 실제 URL 사용
                .build();
    }

    @Bean
    @Qualifier("userClient")
    public WebClient userClient(WebClient.Builder builder) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}

