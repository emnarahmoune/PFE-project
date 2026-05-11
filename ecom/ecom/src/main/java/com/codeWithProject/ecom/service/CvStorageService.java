package com.codeWithProject.ecom.service;



import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface CvStorageService {

    String saveCv(MultipartFile file);

    Path getCvPath(String fileName);

    String extractTextFromCv(Path path, String contentType);

    void deleteCv(String fileName);
}