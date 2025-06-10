package com.comprehensive.eureka.admin.config;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final ChatbotProperties props;

    @Bean
    public WebClient chatbotWebClient(WebClient.Builder builder) {
        return builder.baseUrl(props.getBaseUrl()).build();
    }
}