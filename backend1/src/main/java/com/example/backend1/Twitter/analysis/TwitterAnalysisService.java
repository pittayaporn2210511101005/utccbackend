package com.example.backend1.Twitter.analysis;

import com.example.backend1.Twitter.Tweet;
import com.example.backend1.Twitter.TweetRepository;
import com.example.backend1.Twitter.dto.TweetAnalysisResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class TwitterAnalysisService {

    private final TweetRepository tweetRepository;
    private final TweetAnalysisRepository analysisRepository;
    private final TwitterSentimentAnalyzer sentimentAnalyzer;
    private final TwitterNSFWAnalyzer nsfwAnalyzer;
    private final TwitterTopicClassifier topicClassifier;
    private final TwitterToxicityDetector toxicityDetector;

    private static final int VERSION = 1;

    public TwitterAnalysisService(
            TweetRepository tweetRepository,
            TweetAnalysisRepository analysisRepository,
            TwitterSentimentAnalyzer sentimentAnalyzer,
            TwitterNSFWAnalyzer nsfwAnalyzer,
            TwitterTopicClassifier topicClassifier,
            TwitterToxicityDetector toxicityDetector
    ) {
        this.tweetRepository = tweetRepository;
        this.analysisRepository = analysisRepository;
        this.sentimentAnalyzer = sentimentAnalyzer;
        this.nsfwAnalyzer = nsfwAnalyzer;
        this.topicClassifier = topicClassifier;
        this.toxicityDetector = toxicityDetector;
    }

    @Transactional
    public int analyzeAllPending() {
        List<Tweet> tweets = tweetRepository.findAll();
        int processed = 0;
        for (Tweet tw : tweets) {
            String tid = String.valueOf(tw.getId());
            if (analysisRepository.findByTweetId(tid).isPresent()) continue;
            saveAnalysis(tw);
            processed++;
        }
        return processed;
    }

    @Transactional
    public void reanalyze(String tweetId) {
        Tweet tw = tweetRepository.findById(tweetId)
                .orElseThrow(() -> new IllegalArgumentException("Tweet not found: " + tweetId));
        analysisRepository.findByTweetId(tweetId).ifPresent(analysisRepository::delete);
        saveAnalysis(tw);
    }

    private void saveAnalysis(Tweet tw) {
        String text = tw.getText() == null ? "" : tw.getText().trim();

        String lang = text.codePoints().anyMatch(cp -> cp >= 0x0E00 && cp <= 0x0E7F) ? "th" : "en";

        var sent = sentimentAnalyzer.classify(text);
        var nsfw = nsfwAnalyzer.classify(text);
        double tox = toxicityDetector.toxicityScore(text);
        double hate = toxicityDetector.hateSpeechScore(text);
        String faculty = topicClassifier.detectFaculty(text);
        var topics = topicClassifier.detectTopics(text);

        double confidence = average(sent.score(), 1.0 - (tox * 0.3), 1.0 - (hate * 0.3));

        TweetAnalysis entity = new TweetAnalysis();
        entity.setTweetId(String.valueOf(tw.getId()));
        entity.setLang(lang);
        entity.setSentimentLabel(sent.label());
        entity.setSentimentScore(bd(sent.score()));
        entity.setNsfwLabel(nsfw.label());
        entity.setNsfwScore(bd(nsfw.score()));
        entity.setToxicityScore(bd(tox));
        entity.setHateSpeechScore(bd(hate));
        entity.setFaculty(faculty);
        entity.setTopicsJson(String.join(",", topics));
        entity.setConfidenceOverall(bd(confidence));
        entity.setVersion(VERSION);
        entity.setAnalyzedAt(LocalDateTime.now());

        analysisRepository.save(entity);
    }

    private static BigDecimal bd(double x) {
        // จำกัดสเกล 4 ตำแหน่งให้สอดคล้องกับ DECIMAL(6,4)
        return BigDecimal.valueOf(x).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    private double average(double... xs) {
        double sum = 0;
        for (double x : xs) sum += x;
        return xs.length == 0 ? 0.0 : sum / xs.length;
    }

    public TweetAnalysisResult toDto(TweetAnalysis e) {
        TweetAnalysisResult dto = new TweetAnalysisResult();
        dto.setTweetId(e.getTweetId());
        dto.setLang(e.getLang());
        dto.setSentimentLabel(e.getSentimentLabel());
        dto.setSentimentScore(nz(e.getSentimentScore()));
        dto.setNsfwLabel(e.getNsfwLabel());
        dto.setNsfwScore(nz(e.getNsfwScore()));
        dto.setToxicityScore(nz(e.getToxicityScore()));
        dto.setHateSpeechScore(nz(e.getHateSpeechScore()));
        dto.setFaculty(e.getFaculty());
        dto.setTopics(
                (e.getTopicsJson() == null || e.getTopicsJson().isBlank())
                        ? List.of("อื่นๆ")
                        : Arrays.asList(e.getTopicsJson().split(","))
        );
        dto.setConfidenceOverall(nz(e.getConfidenceOverall()));
        dto.setVersion(e.getVersion() == null ? VERSION : e.getVersion());
        dto.setAnalyzedAt(e.getAnalyzedAt());
        return dto;
    }

    private double nz(BigDecimal bd) {
        return bd == null ? 0.0 : bd.doubleValue();
    }
}
