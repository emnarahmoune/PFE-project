package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.ManagerProfileDTO;

public interface ManagerProfileService {

    ManagerProfileDTO getProfile(String email);

    ManagerProfileDTO updateProfile(String email, ManagerProfileDTO dto);
}