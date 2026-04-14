package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Service - Représente un département ou service de l'entreprise
 *
 * @author Système RH
 * @version 1.0
 */
@Entity
@Table(name = "services",
        indexes = {
                @Index(name = "idx_code_service", columnList = "code_service"),
                @Index(name = "idx_libelle", columnList = "libelle")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_code_service", columnNames = "code_service")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code_service", nullable = false, unique = true, length = 50)
    private String codeService;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @Column(name = "matricule_responsable", length = 100)
    private String matriculeResponsable;

    @Column(name = "budget_annuel")
    private Double budgetAnnuel;

    @Column(name = "effectif_cible")
    private Integer effectifCible;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    // ===== RELATIONS =====
    @OneToMany(mappedBy = "service", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Employe> employes = new ArrayList<>();

    // ===== MÉTHODES MÉTIER =====

    public void ajouterEmploye(Employe employe) {
        if (employe == null) {
            throw new IllegalArgumentException("L'employé ne peut pas être null");
        }

        if (!this.actif) {
            throw new IllegalStateException("Impossible d'ajouter un employé à un service inactif");
        }

        if (this.employes.contains(employe)) {
            return;
        }

        if (employe.getService() != null && !employe.getService().equals(this)) {
            employe.getService().retirerEmploye(employe);
        }

        this.employes.add(employe);
        employe.setService(this);
    }

    public void retirerEmploye(Employe employe) {
        if (employe == null) {
            return;
        }

        if (this.employes.remove(employe)) {
            employe.setService(null);
        }
    }

    public int getNombreEmployesActifs() {
        if (this.employes == null) {
            return 0;
        }
        return (int) this.employes.stream()
                .filter(e -> "ACTIF".equals(e.getStatut()))
                .count();
    }

    public int getNombreEmployes() {
        return this.employes != null ? this.employes.size() : 0;
    }

    @Transient
    public boolean isEffectifCibleAtteint() {
        if (this.effectifCible == null || this.effectifCible == 0) {
            return false;
        }
        return getNombreEmployesActifs() >= this.effectifCible;
    }

    @Transient
    public double getTauxRemplissage() {
        if (this.effectifCible == null || this.effectifCible == 0) {
            return 0.0;
        }
        return (getNombreEmployesActifs() * 100.0) / this.effectifCible;
    }

    public List<Employe> getEmployesActifs() {
        if (this.employes == null) {
            return new ArrayList<>();
        }
        return this.employes.stream()
                .filter(e -> "ACTIF".equals(e.getStatut()))
                .toList();
    }

    @Transient
    public double getMasseSalariale() {
        if (this.employes == null) {
            return 0.0;
        }
        return this.employes.stream()
                .filter(e -> "ACTIF".equals(e.getStatut()))
                .filter(e -> e.getSalaire() != null)
                .mapToDouble(Employe::getSalaire)
                .sum();
    }

    @Transient
    public boolean isBudgetRespect() {
        if (this.budgetAnnuel == null) {
            return true;
        }
        return getMasseSalariale() <= this.budgetAnnuel;
    }

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }

    @Transient
    public String getDescription() {
        return String.format(
                "Service %s - %s | Effectif: %d/%d | Masse salariale: %.2f€ | Budget: %s€ | Statut: %s",
                this.codeService,
                this.libelle,
                getNombreEmployesActifs(),
                this.effectifCible != null ? this.effectifCible : 0,
                getMasseSalariale(),
                this.budgetAnnuel != null ? String.format("%.2f", this.budgetAnnuel) : "Non défini",
                this.actif ? "Actif" : "Inactif"
        );
    }

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        // Initialisation
        if (this.dateCreation == null && this.getId() == null) {
            this.dateCreation = LocalDateTime.now();
        }
        if (this.actif == null) {
            this.actif = true;
        }

        // Validation
        if (this.codeService == null || this.codeService.trim().isEmpty()) {
            throw new IllegalStateException("Le code service est obligatoire");
        }
        if (this.libelle == null || this.libelle.trim().isEmpty()) {
            throw new IllegalStateException("Le libellé du service est obligatoire");
        }

        // Normalisation
        this.codeService = this.codeService.trim().toUpperCase();
        if (this.libelle != null) {
            this.libelle = this.libelle.trim();
        }
    }
}