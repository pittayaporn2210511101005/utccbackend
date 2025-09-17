package com.example.backend1.Twitter;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tweets/search/recent")
                            .queryParam("query", keyword) // ไม่ต้อง encode
                            .queryParam("max_results", "10")
                            .queryParam("tweet.fields", "author_id,created_at")
                            .build())
                    .header("Authorization", "Bearer " + BEARER_TOKEN)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        // แสดง body ของ error จาก API
                                        System.out.println("Error body from Twitter API: " + body);
                                        return Mono.error(new RuntimeException("Twitter API error: " + body));
                                    })
                    )
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred: " + e.getMessage();
        }
    }
}
