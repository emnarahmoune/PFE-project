package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.EmployeFormation;
import com.codeWithProject.ecom.entity.EmployeFormationProgress;
import com.codeWithProject.ecom.entity.Formation;
import com.codeWithProject.ecom.entity.FormationVideo;
import com.codeWithProject.ecom.repository.EmployeFormationRepository;
import com.codeWithProject.ecom.repository.EmployeRepository;
import com.codeWithProject.ecom.service.FormationService;
import com.codeWithProject.ecom.service.dto.EmployeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.codeWithProject.ecom.repository.FormationRepository;
import com.codeWithProject.ecom.repository.EmployeFormationProgressRepository;
import com.codeWithProject.ecom.repository.FormationVideoRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
@RestController
@RequestMapping("/api/formations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FormationController {

    private final FormationService formationService;
    private final EmployeRepository employeRepository;
    private final EmployeFormationRepository employeFormationRepository;

    
    private final FormationVideoRepository formationVideoRepository;
    private final EmployeFormationProgressRepository employeFormationProgressRepository;
    private final FormationRepository formationRepository;
    // =========================
    // CRUD
    // =========================

    @PostMapping
    public Formation create(@RequestBody Formation formation) {
        return formationService.create(formation);
    }

    @GetMapping
    public List<Map<String, Object>> getAll() {
        return formationService.getAll();
    }

    /*
     * IMPORTANT :
     * "\\d+" oblige Spring à accepter seulement des nombres.
     * Donc /mes-formations ne sera plus confondu avec /{id}.
     */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(formationService.getByIdComplete(id));
    }

    /*
     * Routes propres utilisées par Angular :
     * PUT /api/formations/{id}
     * DELETE /api/formations/{id}
     */
    @PutMapping("/{id:\\d+}")
    public Formation update(@PathVariable Long id, @RequestBody Formation formation) {
        return formationService.update(id, formation);
    }

    @DeleteMapping("/{id:\\d+}")
    public void delete(@PathVariable Long id) {
        formationService.delete(id);
    }

    /*
     * Anciennes routes conservées pour compatibilité :
     * PUT /api/formations/id/{id}
     * DELETE /api/formations/id/{id}
     */
    @PutMapping("/id/{id:\\d+}")
    public Formation updateOld(@PathVariable Long id, @RequestBody Formation formation) {
        return formationService.update(id, formation);
    }

    @DeleteMapping("/id/{id:\\d+}")
    public void deleteOld(@PathVariable Long id) {
        formationService.delete(id);
    }

    // =========================
    // DETAILS / PARTICIPANTS
    // =========================

    @GetMapping("/{id:\\d+}/participants")
    public List<EmployeDTO> getParticipants(@PathVariable Long id) {
        return formationService.getParticipants(id);
    }

    @GetMapping("/{id:\\d+}/details")
    public ResponseEntity<Map<String, Object>> getDetails(@PathVariable Long id) {
        return ResponseEntity.ok(formationService.getByIdWithEmployes(id));
    }

    // =========================
    // STATS
    // =========================

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new HashMap<>();

        List<Map<String, Object>> formations = formationService.getAll();

        stats.put("total", formations.size());
        stats.put("actives", formationService.getActives().size());

        int totalParticipants = formations.stream()
                .mapToInt(f -> {
                    Object idObj = f.get("id");

                    if (idObj == null) {
                        return 0;
                    }

                    Long id = Long.valueOf(idObj.toString());
                    return formationService.getParticipants(id).size();
                })
                .sum();

        double dureeMoyenne = formations.stream()
                .mapToInt(f -> {
                    Object duree = f.get("dureeHeures");

                    if (duree == null) {
                        return 0;
                    }

                    return Integer.parseInt(duree.toString());
                })
                .average()
                .orElse(0);

        stats.put("totalParticipants", totalParticipants);
        stats.put("dureeMoyenne", dureeMoyenne);

        return stats;
    }

    // =========================
    // FILTRES
    // =========================

    @GetMapping("/domaine/{domaine}")
    public List<Formation> getByDomaine(@PathVariable String domaine) {
        return formationService.getByDomaine(domaine);
    }

    @GetMapping("/actives")
    public List<Formation> getActives() {
        return formationService.getActives();
    }

    @PutMapping("/{id:\\d+}/activer")
    public void activer(@PathVariable Long id) {
        formationService.activer(id);
    }

    @PutMapping("/{id:\\d+}/desactiver")
    public void desactiver(@PathVariable Long id) {
        formationService.desactiver(id);
    }

    // =========================
    // FORMATIONS UTILISATEUR
    // =========================

    /*
     * Route utilisée par Angular :
     * GET /api/formations/mes-formations
     *
     * Elle retourne EmployeFormation, pas seulement Formation,
     * pour que le front ait :
     * ef.formation
     * ef.progression
     * ef.statut
     */
  @GetMapping("/mes-formations")
public ResponseEntity<List<Map<String, Object>>> getMesFormations(Authentication auth) {
    Employe employe = getCurrentEmploye(auth);

    List<EmployeFormation> suivies =
            employeFormationRepository.findByEmployeId(employe.getId());

    List<Map<String, Object>> response = suivies.stream()
            .filter(ef -> ef.getFormation() != null)
            .map(ef -> {
                Formation f = ef.getFormation();

                Map<String, Object> formationMap = new java.util.HashMap<>();
                formationMap.put("id", f.getId());
                formationMap.put("titre", f.getTitre());
                formationMap.put("description", f.getDescription());
                formationMap.put("domaine", f.getDomaine());
                formationMap.put("dureeHeures", f.getDureeHeures());
                formationMap.put("actif", f.getActif());

                Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", ef.getId());
                item.put("progression", ef.getProgression());
                item.put("statut", ef.getStatut());
                item.put("dateInscription", ef.getDateInscription());
                item.put("formation", formationMap);

                return item;
            })
            .toList();

    return ResponseEntity.ok(response);
}

    /*
     * Ancienne route conservée.
     */
    @GetMapping("/me")
    public List<Formation> getMyFormations(Authentication auth) {
        Employe employe = getCurrentEmploye(auth);

        return formationService.getFormationsByEmploye(employe.getId());
    }

    // =========================
    // INSCRIPTION FORMATION CLASSIQUE
    // =========================

 @PostMapping("/{formationId:\\d+}/inscrire")
public ResponseEntity<?> inscrireFormation(
        @PathVariable Long formationId,
        Authentication auth
) {
    Employe employe = getCurrentEmploye(auth);

    Formation formation = formationRepository.findById(formationId)
            .orElseThrow(() -> new RuntimeException("Formation introuvable id=" + formationId));

    boolean alreadyExists = employeFormationRepository.existsByEmployeIdAndFormationId(
            employe.getId(),
            formation.getId()
    );

    if (!alreadyExists) {
        EmployeFormation employeFormation = new EmployeFormation();
        employeFormation.setEmploye(employe);
        employeFormation.setFormation(formation);
        employeFormation.setStatut("EN_COURS");
        employeFormation.setProgression(0);
        employeFormation.setDateInscription(java.time.LocalDateTime.now());

        employeFormationRepository.save(employeFormation);
    }

    return ResponseEntity.ok(Map.of(
            "message", "Formation inscrite avec succès",
            "formationId", formationId,
            "employeId", employe.getId()
    ));
}

    // =========================
    // RECOMMANDATIONS ANCIENNES / DASHBOARD
    // =========================

    /*
     * Ton dashboard appelait /api/formations/recommendations.
     * On garde cette route pour éviter l'erreur 400.
     */
    @GetMapping("/recommendations")
    public List<Formation> getRecommendations(Authentication auth) {
        Employe employe = getCurrentEmploye(auth);

        return formationService.getRecommendationsAI(employe.getId());
    }

    @GetMapping("/recommendations-skill")
    public List<Formation> getRecommendationsSkill(Authentication auth) {
        Employe employe = getCurrentEmploye(auth);

        return formationService.getRecommendationsBySkills(employe.getId());
    }

    /*
     * Anciennes routes françaises conservées.
     */
    @GetMapping("/recommandations")
    public List<Formation> getRecommandationsGapPoste(Authentication auth) {
        Employe employe = getCurrentEmploye(auth);

        return formationService.getRecommendationsAI(employe.getId());
    }

    @GetMapping("/recommandations-skill")
    public List<Formation> getRecommandationsBoostCompetences(Authentication auth) {
        Employe employe = getCurrentEmploye(auth);

        return formationService.getRecommendationsBySkills(employe.getId());
    }

    // =========================
    // UPLOAD PDF
    // =========================

    @PostMapping("/upload-pdf")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file.getContentType() == null || !file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest().body("Seuls les fichiers PDF sont autorisés");
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get("uploads/" + fileName);

            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            return ResponseEntity.ok(Map.of("filePath", fileName));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur upload PDF");
        }
    }

    // =========================
    // RESET PROGRESS
    // =========================

    /*
     * Route utilisée parfois par le front :
     * POST /api/formations/{formationId}/reset-progress
     */
    @PostMapping("/{formationId:\\d+}/reset-progress")
    public ResponseEntity<?> resetProgressPost(
            @PathVariable Long formationId,
            Authentication auth
    ) {
        formationService.resetProgress(formationId, auth);

        return ResponseEntity.ok().build();
    }

    /*
     * Ancienne route conservée :
     * PUT /api/formations/reset-progress/{formationId}
     */
    @PutMapping("/reset-progress/{formationId:\\d+}")
    public ResponseEntity<?> resetProgress(
            @PathVariable Long formationId,
            Authentication auth
    ) {
        formationService.resetProgress(formationId, auth);

        return ResponseEntity.ok().build();
    }

    // =========================
    // HELPER AUTH
    // =========================

   private Employe getCurrentEmploye(Authentication auth) {
    if (auth == null || auth.getPrincipal() == null) {
        throw new RuntimeException("Utilisateur non authentifié");
    }

    Jwt jwt = (Jwt) auth.getPrincipal();

    String tokenEmail = jwt.getClaimAsString("email");

    if (tokenEmail == null || tokenEmail.isBlank()) {
        tokenEmail = jwt.getClaimAsString("preferred_username");
    }

    if (tokenEmail == null || tokenEmail.isBlank()) {
        throw new RuntimeException("Email utilisateur introuvable dans le token");
    }

    final String finalEmail = tokenEmail;

    return employeRepository.findByEmail(finalEmail)
            .orElseThrow(() -> new RuntimeException("Employé introuvable pour email : " + finalEmail));
}

@GetMapping("/{formationId:\\d+}/completed-videos")
public ResponseEntity<List<Long>> getCompletedVideos(
        @PathVariable Long formationId,
        Authentication auth
) {
    Employe employe = getCurrentEmploye(auth);

    List<EmployeFormationProgress> progressList =
            employeFormationProgressRepository
                    .findByEmployeIdAndVideo_Formation_IdAndCompletedTrue(
                            employe.getId(),
                            formationId
                    );

    List<Long> completedVideoIds = progressList.stream()
            .filter(progress -> progress.getVideo() != null)
            .map(progress -> progress.getVideo().getId())
            .toList();

    return ResponseEntity.ok(completedVideoIds);
}




@GetMapping("/{formationId:\\d+}/videos")
public ResponseEntity<List<FormationVideo>> getVideosByFormation(
        @PathVariable Long formationId
) {
    Formation formation = formationRepository.findById(formationId)
            .orElseThrow(() -> new RuntimeException("Formation introuvable id=" + formationId));

    repairBadVideosIfNeeded(formation);

    return ResponseEntity.ok(
            formationVideoRepository.findByFormation_IdOrderByOrdreAsc(formationId)
    );
}




private void repairBadVideosIfNeeded(Formation formation) {
    Long formationId = formation.getId();

    boolean hasBadSearchVideos =
            formationVideoRepository.existsByFormation_IdAndUrlYoutubeContainingIgnoreCase(
                    formationId,
                    "youtube.com/results"
            );

    if (hasBadSearchVideos) {
        formationVideoRepository.deleteByFormation_IdAndUrlYoutubeContainingIgnoreCase(
                formationId,
                "youtube.com/results"
        );
    }

    List<FormationVideo> videos =
            formationVideoRepository.findByFormation_IdOrderByOrdreAsc(formationId);

    boolean hasGoodVideos = videos.stream()
            .anyMatch(video -> isEmbeddableYoutubeUrl(video.getUrlYoutube()));

    if (hasGoodVideos) {
        return;
    }

    List<FormationVideo> defaultVideos = buildDefaultVideosForFormation(formation);

    for (FormationVideo video : defaultVideos) {
        formationVideoRepository.save(video);
    }
}

private List<FormationVideo> buildDefaultVideosForFormation(Formation formation) {
    String text = (
            safe(formation.getTitre()) + " " +
            safe(formation.getDescription()) + " " +
            safe(formation.getDomaine())
    ).toLowerCase();

    if (text.contains("recrutement") || text.contains("talent")) {
        return List.of(
                video(formation, "Recrutement RH", "https://www.youtube.com/watch?v=HG68Ymazo18", 1),
                video(formation, "Entretien de recrutement", "https://www.youtube.com/watch?v=6G8_qA8M8pQ", 2),
                video(formation, "Sourcing candidats", "https://www.youtube.com/watch?v=4FQY3u4UxS0", 3)
        );
    }

    if (text.contains("paie") || text.contains("salaire")) {
        return List.of(
                video(formation, "Gestion de la paie", "https://www.youtube.com/watch?v=b7OXULhF1pc", 1),
                video(formation, "Bulletin de paie expliqué", "https://www.youtube.com/watch?v=zE51pYOTp2s", 2),
                video(formation, "Charges sociales et salaire net", "https://www.youtube.com/watch?v=5cI-AkKy66I", 3)
        );
    }

    if (text.contains("droit") || text.contains("contrat")) {
        return List.of(
                video(formation, "Droit du travail", "https://www.youtube.com/watch?v=4Ko4b38N7gE", 1),
                video(formation, "Contrat de travail", "https://www.youtube.com/watch?v=O_4LwZ2pJzQ", 2),
                video(formation, "Droit social RH", "https://www.youtube.com/watch?v=R6NoL7cnkQY", 3)
        );
    }

    if (text.contains("leadership") || text.contains("management") || text.contains("équipe") || text.contains("equipe")) {
        return List.of(
                video(formation, "Leadership", "https://www.youtube.com/watch?v=ktlTxC4QG8g", 1),
                video(formation, "Gestion équipe", "https://www.youtube.com/watch?v=4a0FbQdH3dY", 2),
                video(formation, "Management et leadership", "https://www.youtube.com/watch?v=Q2vQkHjS4xQ", 3)
        );
    }

    return List.of(
            video(formation, "Communication professionnelle", "https://www.youtube.com/watch?v=HAnw168huqA", 1),
            video(formation, "Communication efficace au travail", "https://www.youtube.com/watch?v=8sjA90hvnQ0", 2),
            video(formation, "Développement des compétences", "https://www.youtube.com/watch?v=Q2vQkHjS4xQ", 3)
    );
}

private FormationVideo video(Formation formation, String titre, String urlYoutube, Integer ordre) {
    FormationVideo video = new FormationVideo();
    video.setFormation(formation);
    video.setTitre(titre);
    video.setUrlYoutube(urlYoutube);
    video.setOrdre(ordre);
    return video;
}

private boolean isEmbeddableYoutubeUrl(String url) {
    if (url == null || url.isBlank()) {
        return false;
    }

    String lower = url.toLowerCase();

    return lower.contains("youtube.com/watch?v=")
            || lower.contains("youtu.be/")
            || lower.contains("youtube.com/embed/")
            || lower.contains("youtube.com/shorts/");
}


@PostMapping("/videos/{videoId}/complete")
@Transactional
public ResponseEntity<?> completeVideo(
        @PathVariable Long videoId,
        Authentication auth
) {
    Employe employe = getCurrentEmploye(auth);

    FormationVideo video = formationVideoRepository.findById(videoId)
            .orElseThrow(() -> new RuntimeException("Vidéo introuvable id=" + videoId));

    Formation formation = video.getFormation();

    if (formation == null || formation.getId() == null) {
        throw new RuntimeException("Formation introuvable pour la vidéo id=" + videoId);
    }

    boolean inscrit = employeFormationRepository.existsByEmployeIdAndFormationId(
            employe.getId(),
            formation.getId()
    );

    if (!inscrit) {
        throw new RuntimeException("Employé non inscrit à cette formation");
    }

    boolean alreadyCompleted = employeFormationProgressRepository.existsByEmployeIdAndVideo(
            employe.getId(),
            video
    );

    if (!alreadyCompleted) {
        EmployeFormationProgress progress = new EmployeFormationProgress();

        progress.setEmployeId(employe.getId());
        progress.setVideo(video);
        progress.setCompleted(true);

        employeFormationProgressRepository.save(progress);
    }

    long totalVideos = formationVideoRepository
            .findByFormation_IdOrderByOrdreAsc(formation.getId())
            .size();

    long completedVideos = employeFormationProgressRepository
            .countByEmployeIdAndVideo_Formation_IdAndCompletedTrue(
                    employe.getId(),
                    formation.getId()
            );

    int progression = totalVideos == 0
            ? 0
            : (int) Math.round((completedVideos * 100.0) / totalVideos);

    EmployeFormation employeFormation =
            employeFormationRepository.findByEmployeIdAndFormationId(
                    employe.getId(),
                    formation.getId()
            ).orElseThrow(() -> new RuntimeException("Inscription formation introuvable"));

    employeFormation.setProgression(progression);

    if (progression >= 100) {
        employeFormation.setStatut("TERMINEE");
        employeFormation.setDateCompletion(LocalDateTime.now());
    } else {
        employeFormation.setStatut("EN_COURS");
    }

    employeFormationRepository.save(employeFormation);

    return ResponseEntity.ok(Map.of(
            "message", "Vidéo terminée",
            "videoId", video.getId(),
            "formationId", formation.getId(),
            "progression", progression,
            "statut", employeFormation.getStatut()
    ));
}



@GetMapping("/{formationId:\\d+}/certificate")
public ResponseEntity<byte[]> generateCertificate(
        @PathVariable Long formationId,
        Authentication auth
) {
    try {
        Employe employe = getCurrentEmploye(auth);

        EmployeFormation employeFormation =
                employeFormationRepository.findByEmployeIdAndFormationId(
                        employe.getId(),
                        formationId
                ).orElseThrow(() -> new RuntimeException("Formation non trouvée pour cet employé"));

        if (employeFormation.getProgression() == null || employeFormation.getProgression() < 100) {
            throw new RuntimeException("Certificat disponible seulement après 100% de progression");
        }

        Formation formation = employeFormation.getFormation();

        String nomComplet = (safe(employe.getPrenom()) + " " + safe(employe.getNom())).trim();

        if (nomComplet.isBlank()) {
            nomComplet = employe.getEmail();
        }

        String titreFormation = formation != null && formation.getTitre() != null
                ? formation.getTitre()
                : "Formation";

        String domaine = formation != null && formation.getDomaine() != null
                ? formation.getDomaine()
                : "Formation professionnelle";

        Integer duree = formation != null && formation.getDureeHeures() != null
                ? formation.getDureeHeures()
                : 0;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4.rotate(), 0, 0, 0, 0);
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);

        document.open();

        PdfContentByte canvas = writer.getDirectContent();

        float pageWidth = document.getPageSize().getWidth();
        float pageHeight = document.getPageSize().getHeight();

        BaseColor primary = new BaseColor(67, 97, 238);
        BaseColor secondary = new BaseColor(16, 185, 129);
        BaseColor dark = new BaseColor(17, 24, 39);
        BaseColor muted = new BaseColor(100, 116, 139);
        BaseColor lightBg = new BaseColor(248, 250, 252);
        BaseColor tableBg = new BaseColor(245, 247, 251);

        Font brandFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, primary);
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 34, Font.BOLD, dark);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 15, Font.NORMAL, dark);
        Font mutedFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, muted);
        Font nameFont = new Font(Font.FontFamily.HELVETICA, 32, Font.BOLD, primary);
        Font formationFont = new Font(Font.FontFamily.HELVETICA, 23, Font.BOLD, dark);
        Font infoLabelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, dark);
        Font infoValueFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, dark);
        Font signatureFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, dark);
        Font refFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, muted);

        // =========================
        // BACKGROUND
        // =========================
        canvas.setColorFill(lightBg);
        canvas.rectangle(0, 0, pageWidth, pageHeight);
        canvas.fill();

        // Carte blanche
        canvas.setColorFill(BaseColor.WHITE);
        canvas.roundRectangle(45, 35, pageWidth - 90, pageHeight - 70, 24);
        canvas.fill();

        // Bordure principale
        canvas.setColorStroke(primary);
        canvas.setLineWidth(2.3f);
        canvas.roundRectangle(58, 48, pageWidth - 116, pageHeight - 96, 20);
        canvas.stroke();

        // Bordure interne
        canvas.setColorStroke(new BaseColor(203, 213, 225));
        canvas.setLineWidth(0.8f);
        canvas.roundRectangle(78, 68, pageWidth - 156, pageHeight - 136, 16);
        canvas.stroke();

        // Décor haut
        canvas.setColorFill(primary);
        canvas.roundRectangle(105, pageHeight - 100, pageWidth - 210, 9, 5);
        canvas.fill();

        canvas.setColorFill(secondary);
        canvas.roundRectangle(105, pageHeight - 115, pageWidth - 210, 5, 3);
        canvas.fill();

        // Cercles décoratifs
        canvas.setColorFill(new BaseColor(239, 246, 255));
        canvas.circle(125, 125, 55);
        canvas.fill();

        canvas.setColorFill(new BaseColor(236, 253, 245));
        canvas.circle(pageWidth - 120, pageHeight - 115, 65);
        canvas.fill();

        // =========================
        // TEXTES CENTRÉS
        // =========================
        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                new Phrase("Portail RH", brandFont),
                pageWidth / 2,
                pageHeight - 145,
                0
        );

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                new Phrase("CERTIFICAT DE RÉUSSITE", titleFont),
                pageWidth / 2,
                pageHeight - 190,
                0
        );

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                new Phrase("Ce certificat est décerné à", normalFont),
                pageWidth / 2,
                pageHeight - 238,
                0
        );

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                new Phrase(nomComplet, nameFont),
                pageWidth / 2,
                pageHeight - 285,
                0
        );

        // Ligne sous le nom
        canvas.setColorStroke(secondary);
        canvas.setLineWidth(1.6f);
        canvas.moveTo(pageWidth / 2 - 170, pageHeight - 305);
        canvas.lineTo(pageWidth / 2 + 170, pageHeight - 305);
        canvas.stroke();

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                new Phrase("pour avoir terminé avec succès la formation", normalFont),
                pageWidth / 2,
                pageHeight - 345,
                0
        );

        // Titre formation avec retour automatique si long
        PdfPTable formationTitleTable = new PdfPTable(1);
        formationTitleTable.setTotalWidth(560);

        PdfPCell formationTitleCell = new PdfPCell(new Phrase(titreFormation, formationFont));
        formationTitleCell.setBorder(Rectangle.NO_BORDER);
        formationTitleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        formationTitleCell.setPadding(0);
        formationTitleTable.addCell(formationTitleCell);

        formationTitleTable.writeSelectedRows(
                0,
                -1,
                (pageWidth - 560) / 2,
                pageHeight - 375,
                canvas
        );

        // =========================
        // TABLE INFOS
        // =========================
        PdfPTable infoTable = new PdfPTable(3);
infoTable.setTotalWidth(500);
infoTable.setWidths(new float[]{1.3f, 1.0f, 1.1f});

addCertificateInfoCell(infoTable, "Domaine", domaine, infoLabelFont, infoValueFont, tableBg);
addCertificateInfoCell(infoTable, "Durée", duree + " heures", infoLabelFont, infoValueFont, tableBg);
addCertificateInfoCell(infoTable, "Date", LocalDate.now().toString(), infoLabelFont, infoValueFont, tableBg);

infoTable.writeSelectedRows(
        0,
        -1,
        (pageWidth - 500) / 2,
        pageHeight - 455,
        canvas
);

        infoTable.writeSelectedRows(
                0,
                -1,
                (pageWidth - 560) / 2,
                pageHeight - 455,
                canvas
        );

        ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_CENTER,
                new Phrase("Ce certificat valide l’acquisition des connaissances et compétences associées à cette formation.", mutedFont),
                pageWidth / 2,
                140,
                0
        );

        // =========================
// RÉFÉRENCE CERTIFICAT
// =========================
canvas.setColorStroke(new BaseColor(148, 163, 184));
canvas.setLineWidth(0.8f);

canvas.moveTo(pageWidth / 2 - 90, 88);
canvas.lineTo(pageWidth / 2 + 90, 88);
canvas.stroke();

ColumnText.showTextAligned(
        canvas,
        Element.ALIGN_CENTER,
        new Phrase("Référence certificat", signatureFont),
        pageWidth / 2,
        70,
        0
);

ColumnText.showTextAligned(
        canvas,
        Element.ALIGN_CENTER,
        new Phrase("CERT-" + formationId + "-" + employe.getId(), refFont),
        pageWidth / 2,
        54,
        0
);

ColumnText.showTextAligned(
        canvas,
        Element.ALIGN_CENTER,
        new Phrase("Document généré automatiquement par la plateforme PeopleOS Academy", refFont),
        pageWidth / 2,
        24,
        0
);

        document.close();

        byte[] pdfBytes = outputStream.toByteArray();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=certificat-" + formationId + ".pdf")
                .header("Content-Type", "application/pdf")
                .body(pdfBytes);

    } catch (Exception e) {
        throw new RuntimeException("Erreur génération certificat : " + e.getMessage(), e);
    }
}


private void addInfoCell(
        PdfPTable table,
        String label,
        String value,
        Font labelFont,
        Font valueFont
) {
    PdfPCell cell = new PdfPCell();
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setPadding(12);
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setBackgroundColor(new BaseColor(248, 250, 252));

    Paragraph pLabel = new Paragraph(label, labelFont);
    pLabel.setAlignment(Element.ALIGN_CENTER);

    Paragraph pValue = new Paragraph(value, valueFont);
    pValue.setAlignment(Element.ALIGN_CENTER);
    pValue.setSpacingBefore(4);

    cell.addElement(pLabel);
    cell.addElement(pValue);

    table.addCell(cell);
}

private PdfPCell signatureCell(
        String title,
        String value,
        Font titleFont,
        Font valueFont
) {
    PdfPCell cell = new PdfPCell();
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setPadding(10);
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);

    Paragraph line = new Paragraph("____________________________", valueFont);
    line.setAlignment(Element.ALIGN_CENTER);
    line.setSpacingAfter(8);

    Paragraph pTitle = new Paragraph(title, titleFont);
    pTitle.setAlignment(Element.ALIGN_CENTER);

    Paragraph pValue = new Paragraph(value, valueFont);
    pValue.setAlignment(Element.ALIGN_CENTER);

    cell.addElement(line);
    cell.addElement(pTitle);
    cell.addElement(pValue);

    return cell;
}

private String safe(String value) {
    return value == null ? "" : value;
}


private void addCertificateInfoCell(
        PdfPTable table,
        String label,
        String value,
        Font labelFont,
        Font valueFont,
        BaseColor background
) {
    PdfPCell cell = new PdfPCell();
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setPaddingTop(12);
    cell.setPaddingBottom(12);
    cell.setPaddingLeft(8);
    cell.setPaddingRight(8);
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    cell.setBackgroundColor(background);

    Paragraph pLabel = new Paragraph(label, labelFont);
    pLabel.setAlignment(Element.ALIGN_CENTER);
    pLabel.setSpacingAfter(5);

    Paragraph pValue = new Paragraph(value, valueFont);
    pValue.setAlignment(Element.ALIGN_CENTER);

    cell.addElement(pLabel);
    cell.addElement(pValue);

    table.addCell(cell);
}
}