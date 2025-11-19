package com.example.backend1.Pantip;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pantip")
public class PantipController {

    private final PantipScraperService scraperService;

    public PantipController(PantipScraperService scraperService) {
        this.scraperService = scraperService;
    }


    // http://localhost:8082/pantip/fetch?keyword=หอการค้า
    @GetMapping("/fetch")
    public String fetch(@RequestParam String keyword) {
        scraperService.scrapePantip(keyword);
        return "ดึงโพสต์และคอมเมนต์ทั้งหมดของ \"" + keyword + "\" สำเร็จ!";
    }



    //   รีเซ็ตข้อมูลทั้งหมด + เริ่ม ID ที่ 1 ใหม่
    @PostMapping("/reset")
    public String resetData() {
        scraperService.resetPantipData();
        return "  รีเซ็ตข้อมูลทั้งหมดของ Pantip และตั้งค่า ID ให้เริ่มที่ 1 แล้ว!";
    }
}

