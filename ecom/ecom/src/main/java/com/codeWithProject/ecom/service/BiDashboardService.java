package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.*;

public interface BiDashboardService {

    DashboardAdminBiDTO getDashboardAdminBi();

    DashboardManagerBiDTO getManagerDashboardBi(Long managerId);
}