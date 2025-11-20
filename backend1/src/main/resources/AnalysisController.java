package com.example.backend1.Analysis;

import com.example.backend1.Pantip.PantipComment;
import com.example.backend1.Pantip.PantipCommentRepository;
import com.example.backend1.Pantip.PantipPost;
import com.example.backend1.Pantip.PantipPostRepository;
import com.example.backend1.Twitter.Tweet;
import com.example.backend1.Twitter.TweetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

      // <-- โมเดลคณะ
    private final OnnxSentimentService onnx;           // <-- โมเดล sentiment
    private final AnalysisRepository analysisRepo;
    private final TweetRepository tweetRepo;
    private final PantipPostRepository pantipPostRepo;
    private final PantipCommentRepository pantipCommentRepo;

    public AnalysisController(

            OnnxSentimentService onnx,
            AnalysisRepository analysisRepo,
            TweetRepository tweetRepo,
            PantipPostRepository pantipPostRepo,
            PantipCommentRepository pantipCommentRepo
    ) {

        this.onnx = onnx;
        this.analysisRepo = analysisRepo;
        this.tweetRepo = tweetRepo;
        this.pantipPostRepo = pantipPostRepo;
        this.pantipCommentRepo = pantipCommentRepo;
    }

    // ---- test ----
    @GetMapping("/ping")
    public String ping() {
        return "OK - ONNX";
    }

    // ============ วิเคราะห์ข้อความเดียว ============
    @PostMapping("/text")
    public AnalysisResult analyzeSingle(@RequestBody TextRequest request) {

        String text = request.getText();
        log.info("Analyze text (ONNX) = {}", text);

        // --- sentiment ---
        OnnxSentimentService.SentimentResult quick = onnx.analyze(text);

        // --- faculty ---

        AnalysisResult result = new AnalysisResult();
        result.setText(text);
        result.setSentimentLabel(quick.getLabel());
        result.setSentimentScore(quick.getScore());
           // ต้องมี field / setter ชื่อ faculty ใน AnalysisResult

        // ตอนนี้ยังไม่ได้ทำ ABSA ด้วย ONNX → ใส่ null ไปก่อน
        result.setAbsaRaw(null);

        return result;
    }

    // ================================
    //           BATCH MODE
    // ================================
    @PostMapping("/batch/all")
    @Transactional
    public Map<String, Object> batchAll() {

        int t = analyzeTweets();
        int pp = analyzePantipPosts();
        int pc = analyzePantipComments();

        Map<String, Object> m = new HashMap<>();
        m.put("tweets_inserted", t);
        m.put("pantip_posts_inserted", pp);
        m.put("pantip_comments_inserted", pc);

        log.info("Batch summary (ONNX) = {}", m);
        return m;
    }

    // ---------- Tweet ----------
    private int analyzeTweets() {
        List<Tweet> list = tweetRepo.findAll();
        int inserted = 0;

        for (Tweet t : list) {

            String id = String.valueOf(t.getId());

            if (analysisRepo.existsById(id)) continue;

            String text = t.getText();
            if (text == null || text.isBlank()) continue;

            // sentiment + faculty
            OnnxSentimentService.SentimentResult quick = onnx.analyze(text);


            Analysis a = new Analysis();
            a.setId(id);
            a.setText(text);
            a.setCreatedAt(t.getCreatedAt());       // Tweet.createdAt เป็น String
            a.setPlatform("twitter");

            a.setSentiment(quick.getLabel());
            a.setNsfw("normal");
            a.setPoliteness("polite");
            a.setFinalLabel(quick.getLabel());

            analysisRepo.save(a);
            inserted++;
        }

        log.info("analyzeTweets(ONNX) inserted {}", inserted);
        return inserted;
    }

    // ---------- Pantip Post ----------
    private int analyzePantipPosts() {
        List<PantipPost> list = pantipPostRepo.findAll();
        int inserted = 0;

        for (PantipPost p : list) {

            String id = String.valueOf(p.getId());

            if (analysisRepo.existsById(id)) continue;

            String text = p.getContent();
            if (text == null || text.isBlank()) continue;

            // sentiment + faculty
            OnnxSentimentService.SentimentResult quick = onnx.analyze(text);


            Analysis a = new Analysis();
            a.setId(id);
            a.setText(text);
            a.setCreatedAt(p.getPostTime());   // String
            a.setPlatform("pantip_post");

            a.setSentiment(quick.getLabel());
            a.setNsfw("normal");
            a.setPoliteness("polite");
            a.setFinalLabel(quick.getLabel());

            analysisRepo.save(a);
            inserted++;
        }

        log.info("analyzePantipPosts(ONNX) inserted {}", inserted);
        return inserted;
    }

    // ---------- Pantip Comment ----------
    private int analyzePantipComments() {
        List<PantipComment> list = pantipCommentRepo.findAll();
        int inserted = 0;

        for (PantipComment c : list) {

            String id = "cmt-" + c.getId();

            if (analysisRepo.existsById(id)) continue;

            String text = c.getText();
            if (text == null || text.isBlank()) continue;

            // sentiment + faculty
            OnnxSentimentService.SentimentResult quick = onnx.analyze(text);


            Analysis a = new Analysis();
            a.setId(id);
            a.setText(text);
            a.setCreatedAt(c.getCommentedAt());   // String
            a.setPlatform("pantip_comment");

            a.setSentiment(quick.getLabel());
            a.setNsfw("normal");
            a.setPoliteness("polite");
            a.setFinalLabel(quick.getLabel());

            analysisRepo.save(a);
            inserted++;
        }

        log.info("analyzePantipComments(ONNX) inserted {}", inserted);
        return inserted;
    }
}
