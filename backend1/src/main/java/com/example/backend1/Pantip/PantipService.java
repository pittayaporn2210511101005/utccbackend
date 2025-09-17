package com.example.backend1.Pantip;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class PantipService {

    public List<PantipPost> searchPosts(String keyword) {
        List<PantipPost> results = new ArrayList<>();
        WebDriver driver = null;

        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // ไม่เปิดหน้าต่าง Chrome
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");

            driver = new ChromeDriver(options);

            // เข้า Pantip search
            String url = "https://pantip.com/search?q=" + java.net.URLEncoder.encode(keyword, "UTF-8");
            driver.get(url);

            // รอให้กระทู้โหลด
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".pt-list-item__title")));

            List<WebElement> postContainers = driver.findElements(By.cssSelector(".pt-list-item"));

            for (WebElement container : postContainers) {
                try {
                    WebElement titleElement = container.findElement(By.cssSelector(".pt-list-item__title a"));
                    String title = titleElement.getText();
                    String link = titleElement.getAttribute("href");

                    String preview = "";
                    try {
                        WebElement previewElement = container.findElement(By.cssSelector(".pt-list-item__desc"));
                        preview = previewElement.getText();
                    } catch (NoSuchElementException ignored) {}

                    results.add(new PantipPost(title, link, preview));

                } catch (NoSuchElementException ignored) {}
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) driver.quit();
        }

        return results;
    }
}
