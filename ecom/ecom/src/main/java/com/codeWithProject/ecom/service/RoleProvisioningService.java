package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.Employe;

public interface RoleProvisioningService {

    void provisionRole(Employe employe);

    String normalizeRole(String role);
}