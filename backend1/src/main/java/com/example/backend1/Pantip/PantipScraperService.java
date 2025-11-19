package com.example.backend1.Pantip;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.NoSuchElementException;

//ถ้าจะแก้ตรงช่องinput เงื่อนไขมันอยู่หน้านี้ ตั้งแต่บรรทัด224ลงไป
@Service
public class PantipScraperService {

    private final PantipPostRepository postRepo;
    private final PantipCommentRepository commentRepo;
    private List<PantipPost> tempPosts = new ArrayList<>();


    @Autowired
    private JdbcTemplate jdbcTemplate;

    public PantipScraperService(PantipPostRepository postRepo, PantipCommentRepository commentRepo) {
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
    }

    public void scrapePantip(String keyword) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox");
        WebDriver driver = new ChromeDriver(options);

        try {
            int totalCount = 0;
            int page = 1;

            while (true) {
                String searchUrl = "https://pantip.com/search?q=" +
                        URLEncoder.encode(keyword, StandardCharsets.UTF_8) +
                        "&page=" + page;

                driver.get(searchUrl);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".pt-list-item__title a")));

                List<WebElement> posts = driver.findElements(By.cssSelector(".pt-list-item__title a"));
                if (posts.isEmpty()) {
                    System.out.println("🚫 ไม่พบโพสต์ในหน้าที่ " + page);
                    break;
                }

                System.out.println("📄 หน้าที่ " + page + " — พบโพสต์ " + posts.size() + " อัน");

                for (int i = 0; i < posts.size(); i++) {
                    posts = driver.findElements(By.cssSelector(".pt-list-item__title a"));
                    WebElement el = posts.get(i);

                    String title = el.getText();
                    String url = el.getAttribute("href");

                    List<WebElement> previewEls = el.findElements(By.xpath("ancestor::div[contains(@class,'pt-list-item')]//div[@class='pt-list-item__desc']"));
                    String preview = previewEls.isEmpty() ? "" : previewEls.get(0).getText();

                    //   ข้ามโพสต์ซ้ำ
                    Optional<PantipPost> existing = postRepo.findByUrl(url);
                    if (existing.isPresent()) {
                        System.out.println("⚠️ ข้ามโพสต์ซ้ำ: " + title);
                        continue;
                    }

                    driver.get(url);
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".display-post-title")));

                    //  ชื่อผู้ตั้งกระทู้
                    String author = "";
                    try {
                        WebElement authorEl = driver.findElement(By.cssSelector(".display-post-name"));
                        author = authorEl.getText().trim();
                    } catch (NoSuchElementException e) {
                        System.out.println("⚠️ ไม่มีชื่อผู้ตั้งกระทู้ใน: " + title);
                    }

                    //  เนื้อหาโพสต์
                    String content = "";
                    try {
                        content = driver.findElement(By.cssSelector(".display-post-story")).getText();
                    } catch (NoSuchElementException e) {
                        System.out.println("⚠️ ไม่มีเนื้อหาในโพสต์: " + title);
                    }

                    //   เวลาโพสต์
                    String postTime = "";
                    try {
                        WebElement timeEl = driver.findElement(By.cssSelector(".display-post-timestamp"));
                        postTime = timeEl.getText();
                    } catch (NoSuchElementException e) {
                        System.out.println("⚠️ ไม่พบเวลาโพสต์ใน: " + title);
                    }

                    //   บันทึกโพสต์
                    PantipPost post = new PantipPost();
                    post.setTitle(title);
                    post.setUrl(url);
                    post.setPreview(preview);
                    post.setAuthor(author);
                    post.setContent(content);
                    post.setPostTime(postTime);
                    postRepo.save(post);

                    //   โหลดคอมเมนต์
                    try {
                        // Scroll ลงให้คอมเมนต์โหลดครบ
                        long lastHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
                        while (true) {
                            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                            Thread.sleep(1500);
                            long newHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
                            if (newHeight == lastHeight) break;
                            lastHeight = newHeight;
                        }

                        //   Selector ครอบคลุมโครงสร้างทุกแบบ
                        List<WebElement> commentEls = driver.findElements(By.cssSelector(
                                ".display-post-wrapper.section-comment, " +
                                        ".display-post-wrapper.with-top-border.section-comment, " +
                                        ".display-post-comment .display-post-story"
                        ));

                        System.out.println("🟣 พบคอมเมนต์ทั้งหมด " + commentEls.size() + " อัน");

                        for (WebElement commentEl : commentEls) {
                            // 🔹 เนื้อหาคอมเมนต์
                            String text = "";
                            try {
                                text = commentEl.findElement(By.cssSelector(".display-post-story")).getText().trim();
                            } catch (NoSuchElementException ignored) {}

                            // 🔹 ชื่อคนคอมเมนต์
                            String commentAuthor = "";
                            try {
                                commentAuthor = commentEl.findElement(By.cssSelector(".display-post-name")).getText().trim();
                            } catch (NoSuchElementException ignored) {}

                            // 🔹 เวลา comment
                            String commentedAt = "";
                            try {
                                WebElement timeEl = commentEl.findElement(By.cssSelector(".display-post-timestamp abbr"));
                                commentedAt = timeEl.getAttribute("title");
                                try {
                                    DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern(
                                            "dd MMMM yyyy 'เวลา' HH:mm:ss 'น.'", new Locale("th", "TH"));
                                    LocalDateTime dateTime = LocalDateTime.parse(
                                            commentedAt.replace("2568", "2025"), inputFmt);
                                    String formatted = dateTime.format(
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                    commentedAt = formatted;
                                } catch (Exception ignored) {}
                            } catch (NoSuchElementException ignored) {}

                            if (!text.isEmpty()) {
                                PantipComment comment = new PantipComment();
                                comment.setText(text);
                                comment.setAuthor(commentAuthor);
                                comment.setCommentedAt(commentedAt);
                                comment.setPost(post);
                                commentRepo.save(comment);
                            }
                        }

                        System.out.println("💬 บันทึกคอมเมนต์ทั้งหมด " + commentEls.size() + " รายการเรียบร้อย!");

                    } catch (Exception e) {
                        System.out.println("⚠️ โหลดคอมเมนต์ไม่สำเร็จ: " + e.getMessage());
                    }

                    System.out.println("✅ บันทึกโพสต์: " + title + " (" + postTime + ")");
                    totalCount++;

                    driver.navigate().back();
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".pt-list-item__title a")));
                    Thread.sleep(1000);
                }

                // หน้าถัดไป
                try {
                    driver.findElement(By.cssSelector(".pagination .icon-arrow-right, .pagination a.next"));
                    page++;
                } catch (NoSuchElementException e) {
                    System.out.println("🚫 ไม่มีหน้าถัดไปแล้ว (หน้าสุดท้าย)");
                    break;
                }
            }

            System.out.println("🎯 ดึงข้อมูลครบทั้งหมด " + totalCount + " โพสต์เรียบร้อยแล้ว!");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    // ---------------- ล้างข้อมูล + รีเซ็ต ID ----------------
    public void resetPantipData() {
        commentRepo.deleteAll();
        postRepo.deleteAll();
        jdbcTemplate.execute("ALTER TABLE pantip_comment AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE pantip_post AUTO_INCREMENT = 1");
        System.out.println("  รีเซ็ตข้อมูลทั้งหมดและตั้งค่า ID ให้เริ่มที่ 1 ใหม่เรียบร้อยแล้ว!");
    }



    //อันนี้ temp
    public List<PantipPost> scrapePantipTemp(String keyword) {

        tempPosts.clear();  // ล้าง temp ก่อน

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox");
        WebDriver driver = new ChromeDriver(options);

        try {
            int page = 1;

            while (true) {

                String searchUrl = "https://pantip.com/search?q=" +
                        URLEncoder.encode(keyword, StandardCharsets.UTF_8) +
                        "&page=" + page;

                driver.get(searchUrl);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".pt-list-item__title a")));

                List<WebElement> posts = driver.findElements(By.cssSelector(".pt-list-item__title a"));
                if (posts.isEmpty()) break;

                for (int i = 0; i < posts.size(); i++) {

                    posts = driver.findElements(By.cssSelector(".pt-list-item__title a"));
                    WebElement el = posts.get(i);

                    String title = el.getText();
                    String url = el.getAttribute("href");

                    driver.get(url);

                    String author = "";
                    try { author = driver.findElement(By.cssSelector(".display-post-name")).getText(); }
                    catch (Exception e) {}

                    String content = "";
                    try { content = driver.findElement(By.cssSelector(".display-post-story")).getText(); }
                    catch (Exception e) {}

                    String postTime = "";
                    try { postTime = driver.findElement(By.cssSelector(".display-post-timestamp")).getText(); }
                    catch (Exception e) {}

                    PantipPost post = new PantipPost();
                    post.setTitle(title);
                    post.setUrl(url);
                    post.setPreview("");
                    post.setAuthor(author);
                    post.setContent(content);
                    post.setPostTime(postTime);

                    // ⭐ เก็บคอมเมนต์ลงใน list
                    List<PantipComment> commentList = new ArrayList<>();

                    List<WebElement> commentEls = driver.findElements(By.cssSelector(".display-post-wrapper.section-comment"));
                    for (WebElement cEl : commentEls) {
                        try {
                            String text = cEl.findElement(By.cssSelector(".display-post-story")).getText();

                            PantipComment c = new PantipComment();
                            c.setText(text);
                            c.setAuthor("");       // เติมได้ตามต้องการ
                            c.setCommentedAt("");  // เติมได้ตามต้องการ

                            commentList.add(c);

                        } catch (Exception e) {}
                    }

                    post.setComments(commentList);

                    // ⭐ ไม่ save DB → เก็บใน tempPosts
                    tempPosts.add(post);
                }

                page++;
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            driver.quit();
        }

        return tempPosts;
    }//บันทึกลงdb เฉพาะตอนทีกดวิเคราห์
    public void saveTempToDB() {
        for (PantipPost p : tempPosts) {
            PantipPost savedPost = postRepo.save(p);

            for (PantipComment c : p.getComments()) {
                c.setPost(savedPost);
                commentRepo.save(c);
            }
        }
        tempPosts.clear();
    }//คลีนเทม
    public void clearTemp() {
        tempPosts.clear();
    }
    public List<PantipPost> getTemp() {
        return tempPosts;
    }


}

