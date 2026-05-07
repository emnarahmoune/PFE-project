package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.entity.Certificate;
import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.CertificateService;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CertificateController {

    private final CertificateService certificateService;
    private final EmployeRepository employeRepository;

    @PostMapping("/generate/{formationId}")
    public ResponseEntity<byte[]> generate(
            @PathVariable Long formationId,
            Authentication auth) {

        try {
            Jwt jwt = (Jwt) auth.getPrincipal();
            String email = jwt.getClaimAsString("email");

            Employe emp = employeRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Employé introuvable"));

            Certificate cert = certificateService.generateCertificate(emp.getId(), formationId);

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Rectangle pageSize = new Rectangle(1280, 720);
            Document document = new Document(pageSize, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            PdfContentByte canvas = writer.getDirectContent();

            float width = document.getPageSize().getWidth();
            float height = document.getPageSize().getHeight();

            BaseFont regular = BaseFont.createFont(
                    BaseFont.HELVETICA,
                    BaseFont.CP1252,
                    BaseFont.NOT_EMBEDDED
            );

            BaseFont bold = BaseFont.createFont(
                    BaseFont.HELVETICA_BOLD,
                    BaseFont.CP1252,
                    BaseFont.NOT_EMBEDDED
            );

            BaseColor navy = new BaseColor(8, 27, 56);
            BaseColor blue = new BaseColor(43, 98, 246);
            BaseColor blueDark = new BaseColor(43, 84, 214);
            BaseColor green = new BaseColor(20, 184, 132);
            BaseColor gold = new BaseColor(245, 166, 35);
            BaseColor cyan = new BaseColor(24, 170, 242);

            BaseColor pageBg = new BaseColor(232, 238, 247);
            BaseColor white = new BaseColor(255, 255, 255);
            BaseColor dark = new BaseColor(15, 23, 42);
            BaseColor gray = new BaseColor(100, 116, 139);
            BaseColor lightGray = new BaseColor(244, 247, 251);
            BaseColor borderGray = new BaseColor(203, 213, 225);
            BaseColor softBorder = new BaseColor(214, 222, 235);
            BaseColor footerColor = new BaseColor(71, 85, 105);

            // =========================
            // FOND GLOBAL
            // =========================
            canvas.setColorFill(pageBg);
            canvas.rectangle(0, 0, width, height);
            canvas.fill();

            // =========================
            // SIDEBAR GAUCHE
            // =========================
            float sidebarW = 120;
            float blueBarW = 18;

            canvas.setColorFill(navy);
            canvas.rectangle(0, 0, sidebarW, height);
            canvas.fill();

            canvas.setColorFill(blue);
            canvas.rectangle(sidebarW, 0, blueBarW, height);
            canvas.fill();

            // Cercle décoratif
            canvas.setColorFill(cyan);
            canvas.circle(62, height - 82, 34);
            canvas.fill();

            canvas.setColorFill(gold);
            canvas.circle(62, height - 82, 20);
            canvas.fill();

            writeRotated(
                    canvas,
                    bold,
                    "PORTAIL RH CERTIFICATE",
                    52,
                    height / 2,
                    90,
                    15,
                    white
            );

            // =========================
            // ZONE PRINCIPALE
            // =========================
            float mainX = sidebarW + blueBarW;
            float mainW = width - mainX;
            float centerX = mainX + mainW / 2;

            // Fond principal à droite
            canvas.setColorFill(pageBg);
            canvas.rectangle(mainX, 0, mainW, height);
            canvas.fill();

            // Grande carte blanche
            float cardX = mainX + 52;
            float cardY = 52;
            float cardW = mainW - 104;
            float cardH = height - 104;

            canvas.setColorFill(white);
            canvas.roundRectangle(cardX, cardY, cardW, cardH, 24);
            canvas.fill();

            canvas.setColorStroke(softBorder);
            canvas.setLineWidth(2f);
            canvas.roundRectangle(cardX, cardY, cardW, cardH, 24);
            canvas.stroke();

            // Cadre bleu intérieur
            float innerX = cardX + 36;
            float innerY = cardY + 34;
            float innerW = cardW - 72;
            float innerH = cardH - 68;

            canvas.setColorStroke(blue);
            canvas.setLineWidth(2.5f);
            canvas.roundRectangle(innerX, innerY, innerW, innerH, 18);
            canvas.stroke();

            // =========================
            // DÉCORATION HAUT
            // =========================
            float decoLineY = 585;

            canvas.setColorFill(blue);
            canvas.rectangle(centerX - 275, decoLineY, 550, 4);
            canvas.fill();

            canvas.setColorFill(gold);
            canvas.rectangle(centerX - 70, decoLineY - 10, 140, 18);
            canvas.fill();

            drawBadge(
                    canvas,
                    centerX - 145,
                    530,
                    290,
                    42,
                    blueDark,
                    "CERTIFICAT OFFICIEL",
                    bold,
                    white
            );

            // =========================
            // TEXTE PRINCIPAL
            // =========================
            writeCenter(
                    canvas,
                    bold,
                    "CERTIFICAT DE RÉUSSITE",
                    centerX,
                    450,
                    44,
                    dark
            );

            writeCenter(
                    canvas,
                    regular,
                    "Ce certificat est décerné à",
                    centerX,
                    392,
                    22,
                    gray
            );

            writeCenter(
                    canvas,
                    bold,
                    emp.getPrenom() + " " + emp.getNom(),
                    centerX,
                    330,
                    40,
                    green
            );

            // Trait sous le nom
            canvas.setColorStroke(borderGray);
            canvas.setLineWidth(1.2f);
            canvas.moveTo(centerX - 300, 298);
            canvas.lineTo(centerX + 300, 298);
            canvas.stroke();

            writeCenter(
                    canvas,
                    regular,
                    "Pour avoir terminé avec succès la formation",
                    centerX,
                    260,
                    21,
                    dark
            );

            // =========================
            // BLOC FORMATION
            // =========================
            float formationBoxW = 700;
            float formationBoxH = 64;
            float formationBoxX = centerX - formationBoxW / 2;
            float formationBoxY = 180;

            canvas.setColorFill(lightGray);
            canvas.roundRectangle(formationBoxX, formationBoxY, formationBoxW, formationBoxH, 16);
            canvas.fill();

            canvas.setColorStroke(borderGray);
            canvas.setLineWidth(1.2f);
            canvas.roundRectangle(formationBoxX, formationBoxY, formationBoxW, formationBoxH, 16);
            canvas.stroke();

            writeFitCenter(
                    canvas,
                    bold,
                    cert.getFormation().getTitre(),
                    centerX,
                    formationBoxY + 21,
                    30,
                    14,
                    formationBoxW - 60,
                    blue
            );

            // =========================
            // INFOS DU BAS
            // =========================
            float infoY = 100;
            float infoW = 215;
            float infoH = 70;
            float gap = 85;

            float totalInfoW = (infoW * 3) + (gap * 2);
            float startX = centerX - totalInfoW / 2;

            drawInfoBox(
                    canvas,
                    startX,
                    infoY,
                    infoW,
                    infoH,
                    "Code certificat",
                    cert.getCertificateCode(),
                    bold,
                    regular,
                    gray,
                    dark
            );

            drawInfoBox(
                    canvas,
                    startX + infoW + gap,
                    infoY,
                    infoW,
                    infoH,
                    "Date de génération",
                    cert.getDateGeneration().toLocalDate().toString(),
                    bold,
                    regular,
                    gray,
                    dark
            );

            drawInfoBox(
                    canvas,
                    startX + (infoW + gap) * 2,
                    infoY,
                    infoW,
                    infoH,
                    "Signature",
                    "Portail_RH",
                    bold,
                    regular,
                    gray,
                    dark
            );

            // =========================
            // FOOTER
            // =========================
            canvas.setColorStroke(borderGray);
            canvas.setLineWidth(1f);
            canvas.moveTo(centerX - 330, 86);
            canvas.lineTo(centerX + 330, 86);
            canvas.stroke();

            writeCenter(
                    canvas,
                    regular,
                    "Ce document certifie officiellement la réussite de la formation indiquée ci-dessus.",
                    centerX,
                    68,
                    11,
                    footerColor
            );

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.add(
                    "Content-Disposition",
                    "attachment; filename=certificat-" + cert.getCertificateCode() + ".pdf"
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/me")
    public List<Certificate> myCertificates(Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        String email = jwt.getClaimAsString("email");

        Employe emp = employeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employé introuvable"));

        return certificateService.getMyCertificates(emp.getId());
    }

    private void writeCenter(
            PdfContentByte canvas,
            BaseFont font,
            String text,
            float x,
            float y,
            float size,
            BaseColor color) {

        canvas.beginText();
        canvas.setFontAndSize(font, size);
        canvas.setColorFill(color);
        canvas.showTextAligned(Element.ALIGN_CENTER, text, x, y, 0);
        canvas.endText();
    }

    private void writeRotated(
            PdfContentByte canvas,
            BaseFont font,
            String text,
            float x,
            float y,
            float rotation,
            float size,
            BaseColor color) {

        canvas.beginText();
        canvas.setFontAndSize(font, size);
        canvas.setColorFill(color);
        canvas.showTextAligned(Element.ALIGN_CENTER, text, x, y, rotation);
        canvas.endText();
    }

    private void writeFitCenter(
            PdfContentByte canvas,
            BaseFont font,
            String text,
            float x,
            float y,
            float maxSize,
            float minSize,
            float maxWidth,
            BaseColor color) {

        float size = maxSize;

        while (size > minSize && font.getWidthPoint(text, size) > maxWidth) {
            size -= 1;
        }

        writeCenter(canvas, font, text, x, y, size, color);
    }

    private void drawBadge(
            PdfContentByte canvas,
            float x,
            float y,
            float w,
            float h,
            BaseColor bg,
            String text,
            BaseFont font,
            BaseColor textColor) {

        canvas.setColorFill(bg);
        canvas.roundRectangle(x, y, w, h, 18);
        canvas.fill();

        writeCenter(canvas, font, text, x + w / 2, y + 13, 12, textColor);
    }

    private void drawInfoBox(
            PdfContentByte canvas,
            float x,
            float y,
            float w,
            float h,
            String label,
            String value,
            BaseFont labelFont,
            BaseFont valueFont,
            BaseColor labelColor,
            BaseColor valueColor) {

        BaseColor bg = new BaseColor(248, 250, 252);
        BaseColor border = new BaseColor(203, 213, 225);

        canvas.setColorFill(bg);
        canvas.roundRectangle(x, y, w, h, 14);
        canvas.fill();

        canvas.setColorStroke(border);
        canvas.setLineWidth(1.2f);
        canvas.roundRectangle(x, y, w, h, 14);
        canvas.stroke();

        writeCenter(canvas, labelFont, label, x + w / 2, y + h - 23, 10, labelColor);
        writeCenter(canvas, valueFont, value, x + w / 2, y + 20, 15, valueColor);
    }
}