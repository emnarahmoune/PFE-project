package com.codeWithProject.ecom.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DemandeRefusDetailsDTO {
    private Long demandeId;
    private String employePrenom;
    private String employeNom;
    private String employeEmail;
    private String employeDepartement;
    private String managerNom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFin;

    private Integer nbJours;
    private String type;
    private String motifRefus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateSoumission;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateDecisionManager;

    private String commentaireRH;
    private List<String> piecesJustificatives;

    // Constructeur par défaut (nécessaire pour Jackson)
    public DemandeRefusDetailsDTO() {}

    // Constructeur complet (inchangé)
    public DemandeRefusDetailsDTO(Long demandeId, String employePrenom, String employeNom,
                                  String employeEmail, String employeDepartement, String managerNom,
                                  LocalDate dateDebut, LocalDate dateFin, Integer joursOuvres, String type,
                                  String motifRefus, LocalDateTime dateSoumission, LocalDateTime dateRefusManager,
                                  String commentaireRH, String piecesJointes) {
        this.demandeId = demandeId;
        this.employePrenom = employePrenom;
        this.employeNom = employeNom;
        this.employeEmail = employeEmail;
        this.employeDepartement = employeDepartement;
        this.managerNom = managerNom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nbJours = joursOuvres;
        this.type = type;
        this.motifRefus = motifRefus;
        this.dateSoumission = dateSoumission;
        this.dateDecisionManager = dateRefusManager;
        this.commentaireRH = commentaireRH;
        this.piecesJustificatives = (piecesJointes != null && !piecesJointes.isBlank())
                ? List.of(piecesJointes.split(","))
                : List.of();
    }

    // Getters et setters (inchangés, mais assurez-vous qu'ils existent)
    public Long getDemandeId() { return demandeId; }
    public void setDemandeId(Long demandeId) { this.demandeId = demandeId; }
    public String getEmployePrenom() { return employePrenom; }
    public void setEmployePrenom(String employePrenom) { this.employePrenom = employePrenom; }
    public String getEmployeNom() { return employeNom; }
    public void setEmployeNom(String employeNom) { this.employeNom = employeNom; }
    public String getEmployeEmail() { return employeEmail; }
    public void setEmployeEmail(String employeEmail) { this.employeEmail = employeEmail; }
    public String getEmployeDepartement() { return employeDepartement; }
    public void setEmployeDepartement(String employeDepartement) { this.employeDepartement = employeDepartement; }
    public String getManagerNom() { return managerNom; }
    public void setManagerNom(String managerNom) { this.managerNom = managerNom; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public Integer getNbJours() { return nbJours; }
    public void setNbJours(Integer nbJours) { this.nbJours = nbJours; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMotifRefus() { return motifRefus; }
    public void setMotifRefus(String motifRefus) { this.motifRefus = motifRefus; }
    public LocalDateTime getDateSoumission() { return dateSoumission; }
    public void setDateSoumission(LocalDateTime dateSoumission) { this.dateSoumission = dateSoumission; }
    public LocalDateTime getDateDecisionManager() { return dateDecisionManager; }
    public void setDateDecisionManager(LocalDateTime dateDecisionManager) { this.dateDecisionManager = dateDecisionManager; }
    public String getCommentaireRH() { return commentaireRH; }
    public void setCommentaireRH(String commentaireRH) { this.commentaireRH = commentaireRH; }
    public List<String> getPiecesJustificatives() { return piecesJustificatives; }
    public void setPiecesJustificatives(List<String> piecesJustificatives) { this.piecesJustificatives = piecesJustificatives; }
}