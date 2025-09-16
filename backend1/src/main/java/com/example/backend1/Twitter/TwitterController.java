package com.example.backend1.Twitter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TwitterController {

    private final TwitterService twitterService;

    public TwitterController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @GetMapping("/twitters")
    public String getTweets(@RequestParam String keyword) {
        try {
            Thread.sleep(5000); // delay 5 วินาที ระหว่าง request
            return twitterService.searchTweets(keyword);
        } catch (Exception e) {
            e.printStackTrace();  // แสดง error ใน console
            return "Error occurred: " + e.getMessage();
        }
    }
}
