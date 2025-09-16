package com.example.backend1.Twitter;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TwitterService {

    private final WebClient webClient;
    private static final String BEARER_TOKEN = "AAAAAAAAAAAAAAAAAAAAAG7n4AEAAAAAALsl5qhH0XOO7WAOiX%2FRApAusbg%3DWmPWC3FIEqtYlzOyP6qMVwW7Dud2En2nePQbqjLRPRuvvAUnx6";

    public TwitterService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://api.twitter.com/2").build();
    }

    public String searchTweets(String keyword) {
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tweets/search/recent")
                            .queryParam("query", encodedKeyword)
                            .queryParam("max_results", "3")
                            .queryParam("tweet.fields", "author_id,created_at")
                            .build())
                    .header("Authorization", "Bearer " + BEARER_TOKEN)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred: " + e.getMessage();
        }
    }
}