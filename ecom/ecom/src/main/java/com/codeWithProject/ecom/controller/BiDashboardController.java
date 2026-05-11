package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.service.exception.*;
import com.codeWithProject.ecom.service.BiDashboardService;
import com.codeWithProject.ecom.service.dto.DashboardAdminBiDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bi")
@RequiredArgsConstructor
public class BiDashboardController {

 private final BiDashboardService biDashboardService;
    @GetMapping("/dashboard-admin")
    public DashboardAdminBiDTO getDashboardAdminBi() {
        return biDashboardService.getDashboardAdminBi();
    }
}