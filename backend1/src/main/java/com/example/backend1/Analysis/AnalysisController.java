package com.example.backend1.Analysis;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final AnalysisRepository repo;

    public AnalysisController(AnalysisRepository repo) {
        this.repo = repo;
    }

    // คืนข้อมูลทั้งหมดจาก social_analysis → สำหรับ Dashboard
    @GetMapping("/analysis")
    public List<Map<String, Object>> getAnalysis() {

        List<Analysis> rows = repo.findAll();

        return rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("tweetId", r.getId());
            m.put("text", r.getText());
            m.put("sentimentLabel", r.getSentiment());
            m.put("faculty", r.getFaculty());
            m.put("analyzedAt", r.getCreatedAt());
            m.put("topics", List.of("อื่นๆ"));
            m.put("source", r.getPlatform());
            return m;
        }).toList();
    }

    // คืน createdAt สำหรับ Trend Chart
    @GetMapping("/tweet-dates")
    public List<String> getTweetDates() {
        return repo.findAll()
                .stream()
                .map(Analysis::getCreatedAt)
                .toList();
    }
}
