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

    // คืนข้อมูลทั้งหมดจาก social_analysis → สำหรับ Dashboard + Mentions
    @GetMapping("/analysis")
    public List<Map<String, Object>> getAnalysis() {

        List<Analysis> rows = repo.findAll();

        return rows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();

            // สำคัญมาก — React ใช้คีย์ `id`
            m.put("id", r.getId());
            m.put("tweetId", r.getId());      // เผื่อ component อื่นใช้

            m.put("text", r.getText());
            m.put("sentimentLabel", r.getSentiment());
            m.put("faculty", r.getFaculty());
            m.put("analyzedAt", r.getCreatedAt());
            m.put("createdAt", r.getCreatedAt()); // สำหรับ trend chart

            m.put("topics", List.of(r.getText()));
            m.put("source", r.getPlatform());

            m.put("nsfw", r.getNsfw());
            m.put("toxic", r.getPoliteness());
            m.put("finalLabel", r.getFinalLabel());

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

    // อัปเดต sentiment
    @PutMapping("/sentiment/update/{id}")
    public Map<String, Object> updateSentiment(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String newSentiment = body.get("sentiment");

        return repo.findById(id)
                .map(record -> {
                    record.setSentiment(newSentiment);
                    repo.save(record);

                    Map<String, Object> res = new HashMap<>();
                    res.put("status", "success");
                    res.put("id", id);
                    res.put("newSentiment", newSentiment);
                    return res;
                })
                .orElseGet(() -> {
                    Map<String, Object> res = new HashMap<>();
                    res.put("status", "error");
                    res.put("message", "ID not found");
                    res.put("id", id);
                    return res;
                });
    }
    //เรียกหาคณะ
    @GetMapping("/faculty-list")
    public Set<String> getAllFaculty() {
        List<Analysis> rows = repo.findAll();
        Set<String> faculties = new HashSet<>();

        for (Analysis r : rows) {
            if (r.getFaculty() != null) {
                faculties.add(r.getFaculty().trim());
            }
        }
        return faculties;
    }


}
