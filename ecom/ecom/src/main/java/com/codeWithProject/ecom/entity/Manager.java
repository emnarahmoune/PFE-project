package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entité Manager - Représente un manager/responsable RH
 */
@Entity
@Table(name = "managers",
        indexes = {
                @Index(name = "idx_manager_departement", columnList = "departement")
        }
)
@PrimaryKeyJoinColumn(name = "utilisateur_id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@SuperBuilder
@DiscriminatorValue("MANAGER")
public class Manager extends Utilisateur {

    @Column(name = "departement", nullable = false, length = 100)
    private String departement;

    @Column(name = "date_nomination")
    private LocalDate dateNomination;

    // ===== RELATIONS =====

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", unique = true)
    @ToString.Exclude
    private Employe employe;

    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Employe> employesGeres = new ArrayList<>();

    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<DemandeConge> demandesCongeAValider = new ArrayList<>();

    // ===== MÉTHODES MÉTIER =====

    public void ajouterEmploye(Employe employe) {
        if (employe == null) {
            throw new IllegalArgumentException("L'employé ne peut pas être null");
        }

        if (!this.getActif()) {
            throw new IllegalStateException("Manager inactif - impossible d'ajouter des employés");
        }

        if (!this.employesGeres.contains(employe)) {
            this.employesGeres.add(employe);
            employe.setManager(this);
        }
    }

    public void retirerEmploye(Employe employe) {
        if (employe != null && this.employesGeres.remove(employe)) {
            employe.setManager(null);
        }
    }

    public List<Employe> consulterEmploye() {
        return new ArrayList<>(this.employesGeres);
    }

    public List<Employe> consulterEmployesActifs() {
        if (this.employesGeres == null) {
            return new ArrayList<>();
        }
        return this.employesGeres.stream()
                .filter(e -> "ACTIF".equals(e.getStatut()))
                .collect(Collectors.toList());
    }

    public int getNombreEmployesGeres() {
        return consulterEmployesActifs().size();
    }

    public int getNombreEmployesTotal() {
        return this.employesGeres != null ? this.employesGeres.size() : 0;
    }

    public List<DemandeConge> consulterDemandesConge() {
        return new ArrayList<>(this.demandesCongeAValider);
    }

    public List<DemandeConge> consulterDemandesEnAttente() {
        if (this.demandesCongeAValider == null) {
            return new ArrayList<>();
        }
        return this.demandesCongeAValider.stream()
                .filter(d -> "EN_ATTENTE".equals(d.getStatut()))
                .sorted((d1, d2) -> {
                    if (d1.getDateDemande() == null) return -1;
                    if (d2.getDateDemande() == null) return 1;
                    return d1.getDateDemande().compareTo(d2.getDateDemande());
                })
                .collect(Collectors.toList());
    }

    public void validerDemandeConge(DemandeConge demande) {
        if (demande == null) {
            throw new IllegalArgumentException("La demande ne peut pas être null");
        }

        if (!this.getActif()) {
            throw new IllegalStateException("Manager inactif - impossible de valider des demandes");
        }

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new IllegalStateException(
                    String.format("Seules les demandes EN_ATTENTE peuvent être validées (statut actuel: %s)",
                            demande.getStatut())
            );
        }

        if (!this.employesGeres.contains(demande.getEmploye())) {
            throw new IllegalStateException("Cette demande ne concerne pas un employé sous votre responsabilité");
        }

        demande.valider();
        demande.setManager(this);
        demande.notifierDecision();
    }

    public void refuserDemandeConge(DemandeConge demande, String motif) {
        if (demande == null) {
            throw new IllegalArgumentException("La demande ne peut pas être null");
        }

        if (!this.getActif()) {
            throw new IllegalStateException("Manager inactif - impossible de refuser des demandes");
        }

        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new IllegalStateException("Seules les demandes EN_ATTENTE peuvent être refusées");
        }

        if (!this.employesGeres.contains(demande.getEmploye())) {
            throw new IllegalStateException("Cette demande ne concerne pas un employé sous votre responsabilité");
        }

        if (motif != null && !motif.trim().isEmpty()) {
            demande.refuserAvecMotif(motif);
        } else {
            demande.refuser();
        }

        demande.setManager(this);
        demande.notifierDecision();
    }

    public long getNombreDemandesEnAttente() {
        return consulterDemandesEnAttente().size();
    }

    public List<DemandeConge> getDemandesUrgentes() {
        LocalDate dateLimite = LocalDate.now().plusDays(7);

        return consulterDemandesEnAttente().stream()
                .filter(d -> d.getDateDebut() != null && d.getDateDebut().isBefore(dateLimite))
                .collect(Collectors.toList());
    }

    public void consulterDashboardsRH() {
        if (!this.getActif()) {
            throw new IllegalStateException("Manager inactif");
        }
    }

    public void consulterScoresRisque() {
        if (!this.getActif()) {
            throw new IllegalStateException("Manager inactif");
        }
    }

    @Transient
    public String genererRapportEquipe() {
        StringBuilder rapport = new StringBuilder();
        rapport.append("=== RAPPORT D'ÉQUIPE ===\n");
        rapport.append("Manager: ").append(this.getPrenom()).append(" ").append(this.getNom()).append("\n");
        rapport.append("Département: ").append(this.departement).append("\n");
        rapport.append("Effectif actif: ").append(getNombreEmployesGeres()).append("\n");
        rapport.append("Demandes en attente: ").append(getNombreDemandesEnAttente()).append("\n");

        long demandesUrgentes = getDemandesUrgentes().size();
        if (demandesUrgentes > 0) {
            rapport.append("⚠️ Demandes urgentes: ").append(demandesUrgentes).append("\n");
        }

        return rapport.toString();
    }

    @Transient
    public String getNomComplet() {
        return this.getPrenom() + " " + this.getNom();
    }

    @Transient
    public String getMatricule() {
        return this.employe != null ? this.employe.getMatricule() : null;
    }

    @Transient
    public long getAncienneteManager() {
        if (this.dateNomination == null) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(this.dateNomination, LocalDate.now());
    }

    public void activer() {
        this.setActif(true);
    }

    public void desactiver() {
        this.setActif(false);
    }

    // ===== INITIALISATION =====
    @PostLoad
    @PostPersist
    protected void initLists() {
        if (employesGeres == null) {
            employesGeres = new ArrayList<>();
        }
        if (demandesCongeAValider == null) {
            demandesCongeAValider = new ArrayList<>();
        }
    }
}