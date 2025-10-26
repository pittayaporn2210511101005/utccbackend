package com.example.backend1.Twitter.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TweetAnalysisRepository extends JpaRepository<TweetAnalysis, Long> {
    Optional<TweetAnalysis> findByTweetId(String tweetId); // ← String ให้ตรงกับ Tweet.id
    List<TweetAnalysis> findBySentimentLabel(String label);
}
