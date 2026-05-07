package org.example.aiintegratedspringbootservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(@Value("${openrouter.api.key}") String apiKey,
                                 @Value("${openrouter.base-url}") String baseUrl) {

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
