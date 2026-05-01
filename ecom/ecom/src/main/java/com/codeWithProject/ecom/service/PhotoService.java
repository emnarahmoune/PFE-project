// service/PhotoService.java
package com.codeWithProject.ecom.service;

import org.springframework.web.multipart.MultipartFile;

public interface PhotoService {
    String uploadPhoto(Long userId, MultipartFile file);
    byte[] getPhoto(String photoUrl);
    void deletePhoto(Long userId);
}