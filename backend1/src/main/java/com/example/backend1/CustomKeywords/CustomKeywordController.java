package com.example.backend1.CustomKeywords;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/custom-keywords")
public class CustomKeywordController {

    private final CustomKeywordRepo repo;

    public CustomKeywordController(CustomKeywordRepo repo) {
        this.repo = repo;
    }

    @PostMapping("/add")
    public CustomKeyword add(@RequestBody CustomKeyword ck) {
        return repo.save(ck);
    }

    @GetMapping("/all")
    public List<CustomKeyword> all() {
        return repo.findAll();
    }
}
