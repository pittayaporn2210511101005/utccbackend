package com.example.backend1.Pantip;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pantip")
public class PantipController {

    private final PantipScraperService scraperService;

    public PantipController(PantipScraperService scraperService) {
        this.scraperService = scraperService;
    }


     //ตอนใช้ดึงในpostman ไม่ต้องสนใจช่องอินพุดของกูอยุ่ด้านล่าง
    @GetMapping("/fetch")
    public String fetch(@RequestParam String keyword) {
        scraperService.scrapePantip(keyword);
        return "ดึงโพสต์และคอมเมนต์ทั้งหมดของ \"" + keyword + "\" สำเร็จ!";
    }


    //   รีเซ็ตข้อมูลทั้งหมด อย่าใส่ใจอันนี้ กูเอาไว้เทสdb
    @PostMapping("/reset")
    public String resetData() {
        scraperService.resetPantipData();
        return "  รีเซ็ตข้อมูลทั้งหมดของ Pantip และตั้งค่า ID ให้เริ่มที่ 1 แล้ว!";
    }

    // http://localhost:8082/pantip/search-and-save?keyword=หอการค้า
    @GetMapping("/search-and-save")
    public String searchAndSave(@RequestParam String keyword) {
        scraperService.scrapePantip(keyword);
        return "✅ ดึงข้อมูลและบันทึกลงฐานข้อมูลด้วยคำว่า: " + keyword;
    }

    //หลังจากบันทัดนี้คือ ไอtemp ที่เอาไว้ค้นหา ถ้าจะแก้ให้แก้หลังจากนี้
    // เรียก temp แต่ไม่บันทึกแค่แสดงให้ดู
    @GetMapping("/temp-fetch")
    public List<PantipPost> tempFetch(@RequestParam String keyword) {
        scraperService.clearTemp();
        List<PantipPost> results = scraperService.scrapePantipTemp(keyword);
        return results;   // ส่งให้ React แสดงผล
    }
    //จะบันทึกหลังผู้ใช้กดวิเคราห์
    @PostMapping("/save-temp")
    public String saveTemp() {
        scraperService.saveTempToDB();
        return "บันทึกสำเร็จ!";
    }
    @PostMapping("/clear-temp")
    public String clearTemp() {
        scraperService.clearTemp();
        return "ยกเลิกสำเร็จ!";
    }
}

