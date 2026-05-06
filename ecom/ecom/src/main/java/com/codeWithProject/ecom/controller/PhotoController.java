package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.service.PhotoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
@Slf4j
public class PhotoController {

    private final PhotoService photoService;

    @GetMapping("/{fileName}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable String fileName) {
        byte[] image = photoService.getPhoto(fileName);

        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.IMAGE_JPEG;

        String lower = fileName.toLowerCase();

        if (lower.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".gif")) {
            mediaType = MediaType.IMAGE_GIF;
        } else if (lower.endsWith(".webp")) {
            mediaType = MediaType.parseMediaType("image/webp");
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                .body(image);
    }
}