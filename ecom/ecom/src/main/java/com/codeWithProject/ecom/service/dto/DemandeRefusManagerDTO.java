package com.codeWithProject.ecom.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeRefusManagerDTO {

    private Long id;

    private Long employeId;
    private String employePrenom;
    private String employeNom;
    private String employeEmail;
    private String employeDepartement;

    private String managerNom;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private String motifRefus;
    private String statut;
    private LocalDate dateDecision;

    private String photoUrl;
    private String employePhotoProfil;
    private String employePhotoUrl;

    /*
     * Constructeur pour ancienne requête JPQL :
     * new DemandeRefusManagerDTO(
     *   d.id,
     *   e.prenom,
     *   e.nom,
     *   e.departement,
     *   CONCAT(m.prenom, ' ', m.nom),
     *   d.dateDebut,
     *   d.dateFin,
     *   d.motifRefus
     * )
     */
    public DemandeRefusManagerDTO(
            Long id,
            String employePrenom,
            String employeNom,
            String employeDepartement,
            String managerNom,
            LocalDate dateDebut,
            LocalDate dateFin,
            String motifRefus
    ) {
        this.id = id;
        this.employePrenom = employePrenom;
        this.employeNom = employeNom;
        this.employeDepartement = employeDepartement;
        this.managerNom = managerNom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.motifRefus = motifRefus;
    }

    /*
     * Constructeur pour nouvelle requête JPQL :
     * new DemandeRefusManagerDTO(
     *   d.id,
     *   e.id,
     *   e.prenom,
     *   e.nom,
     *   e.email,
     *   e.departement,
     *   CONCAT(COALESCE(m.prenom, ''), ' ', COALESCE(m.nom, '')),
     *   d.dateDebut,
     *   d.dateFin,
     *   d.motifRefus,
     *   d.statut,
     *   d.dateDecision,
     *   e.photoUrl
     * )
     */
    public DemandeRefusManagerDTO(
            Long id,
            Long employeId,
            String employePrenom,
            String employeNom,
            String employeEmail,
            String employeDepartement,
            String managerNom,
            LocalDate dateDebut,
            LocalDate dateFin,
            String motifRefus,
            String statut,
            LocalDate dateDecision,
            String photoUrl
    ) {
        this.id = id;
        this.employeId = employeId;
        this.employePrenom = employePrenom;
        this.employeNom = employeNom;
        this.employeEmail = employeEmail;
        this.employeDepartement = employeDepartement;
        this.managerNom = managerNom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.motifRefus = motifRefus;
        this.statut = statut;
        this.dateDecision = dateDecision;

        this.photoUrl = photoUrl;
        this.employePhotoProfil = photoUrl;
        this.employePhotoUrl = photoUrl;
    }
}