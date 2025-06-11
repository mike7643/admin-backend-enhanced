package com.comprehensive.eureka.admin.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @Qualifier("chatbotClient")
    public WebClient chatbotClient(WebClient.Builder builder) {
        return builder
                .baseUrl("${services.base-url}")
                .build();
    }

    @Bean
    @Qualifier("userClient")
    public WebClient userClient(WebClient.Builder builder) {
        return builder
                .baseUrl("${services.base-url}")
                .build();
    }
}
