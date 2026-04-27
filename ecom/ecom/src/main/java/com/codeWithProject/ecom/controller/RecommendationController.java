package com.codeWithProject.ecom.controller;
import com.codeWithProject.ecom.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/formations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService service;

    @GetMapping("/recommendations/{id}")
    public List<Map<String, Object>> get(@PathVariable Long id) {
        return service.recommend(id);
    }


}