package com.example.backend1.Twitter.analysis;

import com.example.backend1.Twitter.dto.TweetAnalysisResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/twitter/analysis")
public class TwitterAnalysisController {

    private final TwitterAnalysisService service;
    private final TweetAnalysisRepository repo;

    public TwitterAnalysisController(TwitterAnalysisService service, TweetAnalysisRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @PostMapping("/run")
    public String runAll() {
        int n = service.analyzeAllPending();
        return "Analyzed " + n + " tweets.";
    }

    @GetMapping
    public List<TweetAnalysisResult> list() {
        return repo.findAll().stream().map(service::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{tweetId}")
    public TweetAnalysisResult byTweetId(@PathVariable String tweetId) {
        TweetAnalysis entity = repo.findByTweetId(tweetId)
                .orElseThrow(() -> new IllegalArgumentException("No analysis for tweetId " + tweetId));
        return service.toDto(entity);
    }

    @PostMapping("/{tweetId}/reprocess")
    public String reprocess(@PathVariable String tweetId) {
        service.reanalyze(tweetId);
        return "Reprocessed tweetId " + tweetId;
    }
}
