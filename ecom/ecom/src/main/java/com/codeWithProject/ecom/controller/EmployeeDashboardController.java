package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.service.EmployeeDashboardService;
import com.codeWithProject.ecom.service.dto.EmployeeDashboardStatsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/employee-dashboard")
@RequiredArgsConstructor
public class EmployeeDashboardController {

    private final EmployeeDashboardService employeeDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMesStats() {
        EmployeeDashboardStatsDTO stats = employeeDashboardService.getMesStats();

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "data", stats
                )
        );
    }
}