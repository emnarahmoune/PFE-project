package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.service.dto.*;
import com.codeWithProject.ecom.repository.BiDashboardRepository;
import com.codeWithProject.ecom.service.BiDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BiDashboardServiceImpl implements BiDashboardService {

    private final BiDashboardRepository biDashboardRepository;

    @Override
    public DashboardAdminBiDTO getDashboardAdminBi() {
        return biDashboardRepository.getDashboardAdminBi();
    }

    @Override
public DashboardManagerBiDTO getManagerDashboardBi(Long managerId) {
    return biDashboardRepository.getManagerDashboardBi(managerId);
}
}