package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.service.CvStorageService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CvStorageServiceImpl implements CvStorageService {

    @Value("${recrutement.cv.upload-dir:uploads/cv}")
    private String uploadDir;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @Override
    public String saveCv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Le fichier CV est obligatoire.");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new RuntimeException("Format CV non supporté. Utilisez PDF, DOC ou DOCX.");
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "cv";

            String extension = getExtension(originalName);
            String storedFileName = UUID.randomUUID() + extension;

            Path destination = uploadPath.resolve(storedFileName).normalize();

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return storedFileName;

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du CV.", e);
        }
    }

    @Override
    public Path getCvPath(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new RuntimeException("Nom du fichier CV invalide.");
        }

        return Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize()
                .resolve(fileName)
                .normalize();
    }

    @Override
    public String extractTextFromCv(Path path, String contentType) {
        if (path == null || !Files.exists(path)) {
            return "";
        }

        try {
            if ("application/pdf".equals(contentType)) {
                return extractTextFromPdf(path);
            }

            if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
                return extractTextFromDocx(path);
            }

            if ("application/msword".equals(contentType)) {
                return extractTextFromDoc(path);
            }

            return "";

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction du texte du CV.", e);
        }
    }

    @Override
    public void deleteCv(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }

        try {
            Path path = getCvPath(fileName);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la suppression du CV.", e);
        }
    }

    private String extractTextFromPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return cleanText(stripper.getText(document));
        }
    }

    private String extractTextFromDocx(Path path) throws IOException {
        try (
                InputStream inputStream = Files.newInputStream(path);
                XWPFDocument document = new XWPFDocument(inputStream);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)
        ) {
            return cleanText(extractor.getText());
        }
    }

    private String extractTextFromDoc(Path path) throws IOException {
        try (
                InputStream inputStream = Files.newInputStream(path);
                HWPFDocument document = new HWPFDocument(inputStream);
                WordExtractor extractor = new WordExtractor(document)
        ) {
            return cleanText(extractor.getText());
        }
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replaceAll("\\u0000", " ")
                .replaceAll("[\\t\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll(" {2,}", " ")
                .trim();
    }

    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');

        if (index == -1) {
            return "";
        }

        return fileName.substring(index);
    }
}