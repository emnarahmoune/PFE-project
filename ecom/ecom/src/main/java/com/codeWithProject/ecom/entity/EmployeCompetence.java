package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entité EmployeCompetence - Table d'association entre Employe et Competence
 */
@Entity
@Table(name = "employe_competence",
        indexes = {
                @Index(name = "idx_ec_employe", columnList = "employe_id"),
                @Index(name = "idx_ec_competence", columnList = "competence_id"),
                @Index(name = "idx_ec_niveau", columnList = "niveau"),
                @Index(name = "idx_ec_certifie", columnList = "certifie")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employe_competence",
                        columnNames = {"employe_id", "competence_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeCompetence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "niveau", length = 50, nullable = false)
    @Builder.Default
    private String niveau = "DEBUTANT";

    @Column(name = "date_acquisition")
    private LocalDate dateAcquisition;

    @Column(name = "certifie", nullable = false)
    @Builder.Default
    private Boolean certifie = false;

    @Column(name = "date_certification")
    private LocalDate dateCertification;

    @Column(name = "organisme_certification", length = 200)
    private String organismeCertification;

    @Column(name = "numero_certificat", length = 100)
    private String numeroCertificat;

    @Column(name = "date_expiration_certification")
    private LocalDate dateExpirationCertification;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "valide_par_manager", nullable = false)
    @Builder.Default
    private Boolean valideParManager = false;

    @Column(name = "date_validation")
    private LocalDate dateValidation;




    // ===== RELATIONS =====
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    @ToString.Exclude
    private Employe employe;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "competence_id", nullable = false)
    @ToString.Exclude
    private Competence competence;

    // ===== CONSTANTES =====
    private static final String[] NIVEAUX_VALIDES = {"DEBUTANT", "INTERMEDIAIRE", "AVANCE", "EXPERT"};

    // ===== MÉTHODES MÉTIER =====

    public void affecterCompetence(Employe employe, Competence competence, String niveau) {
        if (employe == null || competence == null) {
            throw new IllegalArgumentException("Employé et compétence obligatoires");
        }

        this.employe = employe;
        this.competence = competence;
        this.niveau = niveau != null ? niveau.toUpperCase() : "DEBUTANT";
        this.dateAcquisition = LocalDate.now();
    }

    public void modifierNiveau(String nouveauNiveau) {
        if (nouveauNiveau == null || nouveauNiveau.trim().isEmpty()) {
            throw new IllegalArgumentException("Le niveau ne peut pas être vide");
        }

        String niveauUpper = nouveauNiveau.trim().toUpperCase();

        if (!isNiveauValide(niveauUpper)) {
            throw new IllegalArgumentException(
                    String.format("Niveau invalide '%s'. Valeurs acceptées: DEBUTANT, INTERMEDIAIRE, AVANCE, EXPERT",
                            nouveauNiveau)
            );
        }

        this.niveau = niveauUpper;
    }

    public void certifier(String organisme, String numeroCertificat, LocalDate dateExpiration) {
        this.certifie = true;
        this.dateCertification = LocalDate.now();
        this.organismeCertification = organisme;
        this.numeroCertificat = numeroCertificat;
        this.dateExpirationCertification = dateExpiration;
    }

    public void retirerCertification() {
        this.certifie = false;
        this.dateCertification = null;
        this.organismeCertification = null;
        this.numeroCertificat = null;
        this.dateExpirationCertification = null;
    }

    public void validerParManager() {
        this.valideParManager = true;
        this.dateValidation = LocalDate.now();
    }

    public void invalider() {
        this.valideParManager = false;
        this.dateValidation = null;
    }

    // ===== MÉTHODES DE VALIDATION =====

    private boolean isNiveauValide(String niveau) {
        if (niveau == null) return false;
        for (String niveauValide : NIVEAUX_VALIDES) {
            if (niveauValide.equals(niveau)) {
                return true;
            }
        }
        return false;
    }

    // ===== MÉTHODES CALCULÉES =====

    @Transient
    public long getAncienneteEnMois() {
        if (this.dateAcquisition == null) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(this.dateAcquisition, LocalDate.now());
    }

    @Transient
    public long getAncienneteEnAnnees() {
        return getAncienneteEnMois() / 12;
    }

    @Transient
    public boolean doitEtreRecertifiee() {
        if (!Boolean.TRUE.equals(this.certifie) || this.dateCertification == null) {
            return false;
        }

        if (this.dateExpirationCertification != null) {
            return LocalDate.now().isAfter(this.dateExpirationCertification);
        }

        long moisDepuisCertification = ChronoUnit.MONTHS.between(this.dateCertification, LocalDate.now());
        return moisDepuisCertification >= 24;
    }

    @Transient
    public boolean isCertificationExpiree() {
        if (!Boolean.TRUE.equals(this.certifie) || this.dateExpirationCertification == null) {
            return false;
        }
        return LocalDate.now().isAfter(this.dateExpirationCertification);
    }

    @Transient
    public int getScoreNiveau() {
        if (this.niveau == null) return 0;
        switch (this.niveau) {
            case "EXPERT": return 4;
            case "AVANCE": return 3;
            case "INTERMEDIAIRE": return 2;
            case "DEBUTANT": return 1;
            default: return 0;
        }
    }

    @Transient
    public String getDescription() {
        StringBuilder desc = new StringBuilder();

        if (this.competence != null && this.competence.getNom() != null) {
            desc.append(this.competence.getNom());
        } else {
            desc.append("Compétence inconnue");
        }

        desc.append(" - Niveau: ").append(this.niveau != null ? this.niveau : "N/A");

        if (Boolean.TRUE.equals(this.certifie)) {
            desc.append(" ✓ Certifié");
            if (isCertificationExpiree()) {
                desc.append(" (EXPIRÉ)");
            }
        }

        if (this.dateAcquisition != null) {
            desc.append(" - Acquis le: ").append(this.dateAcquisition);
            desc.append(" (").append(getAncienneteEnAnnees()).append(" an");
            if (getAncienneteEnAnnees() > 1) desc.append("s");
            desc.append(")");
        }

        if (!Boolean.TRUE.equals(this.valideParManager)) {
            desc.append(" ⚠️ Non validé");
        }

        return desc.toString();
    }

    // ===== LIFECYCLE CALLBACKS =====

    @PrePersist
    protected void onCreate() {
        if (this.dateAcquisition == null) {
            this.dateAcquisition = LocalDate.now();
        }
        if (this.niveau == null || this.niveau.trim().isEmpty()) {
            this.niveau = "DEBUTANT";
        } else {
            this.niveau = this.niveau.toUpperCase();
        }
        if (this.certifie == null) {
            this.certifie = false;
        }
        if (this.valideParManager == null) {
            this.valideParManager = false;
        }

        // Validation du niveau
        if (!isNiveauValide(this.niveau)) {
            throw new IllegalStateException("Niveau invalide: " + this.niveau);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.niveau != null) {
            this.niveau = this.niveau.toUpperCase();
        }

        // Validation du niveau
        if (!isNiveauValide(this.niveau)) {
            throw new IllegalStateException("Niveau invalide: " + this.niveau);
        }

        if (Boolean.TRUE.equals(this.certifie) && this.dateCertification == null) {
            this.dateCertification = LocalDate.now();
        }
    }
}