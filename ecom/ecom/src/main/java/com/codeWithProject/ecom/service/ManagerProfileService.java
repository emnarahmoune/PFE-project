package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.ChangePasswordRequest;
import com.codeWithProject.ecom.service.dto.ManagerProfileDTO;
import org.springframework.security.oauth2.jwt.Jwt;

public interface ManagerProfileService {

    ManagerProfileDTO getProfile(Jwt jwt);

    ManagerProfileDTO updateProfile(Jwt jwt, ManagerProfileDTO dto);

    void changePassword(Jwt jwt, ChangePasswordRequest dto);
}