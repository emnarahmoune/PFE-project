package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Slf4j
@Entity
@Table(name = "demandes_conge",
        indexes = {
                @Index(name = "idx_dc_employe", columnList = "employe_id"),
                @Index(name = "idx_dc_manager", columnList = "manager_id"),
                @Index(name = "idx_dc_statut",  columnList = "statut"),
                @Index(name = "idx_dc_type",    columnList = "type"),
                @Index(name = "idx_dc_dates",   columnList = "date_debut, date_fin")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "statut", nullable = false, length = 50)
    @Builder.Default
    private String statut = "EN_ATTENTE";

    @Column(name = "date_demande")
    private LocalDate dateDemande;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_decision")
    private LocalDate dateDecision;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    @Column(name = "jours_ouvres")
    private Integer joursOuvres;

    @Column(name = "urgente")
    @Builder.Default
    private Boolean urgente = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"demandesConge", "manager", "competences", "formations"})
    private Employe employe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    @ToString.Exclude
    @JsonIgnoreProperties({"demandesCongeAValider", "employesGeres"})
    private Manager manager;

    // ✅ Supprimer les déclarations en double - garder UNE SEULE fois ces champs
    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;

    @Column(name = "task_id", length = 100)
    private String currentTaskId;

    private static final Set<DayOfWeek> WEEKEND = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    private static final Set<LocalDate> JOURS_FERIES = Set.of(
            LocalDate.of(2026, 1, 1),   LocalDate.of(2026, 4, 6),
            LocalDate.of(2026, 5, 1),   LocalDate.of(2026, 5, 8),
            LocalDate.of(2026, 5, 14),  LocalDate.of(2026, 5, 25),
            LocalDate.of(2026, 7, 14),  LocalDate.of(2026, 8, 15),
            LocalDate.of(2026, 11, 1),  LocalDate.of(2026, 11, 11),
            LocalDate.of(2026, 12, 25)
    );

    public void soumettre() {
        validerDates();
        validerType();
        this.joursOuvres = calculerJoursOuvres();
        verifierSoldeSuffisant();
        this.dateDemande = LocalDate.now();
        this.statut = "EN_ATTENTE";
        this.urgente = ChronoUnit.DAYS.between(LocalDate.now(), this.dateDebut) < 7;
    }

    public void modifier(LocalDate nouvelleDateDebut, LocalDate nouvelleDateFin,
                         String nouveauType, String nouveauCommentaire) {
        verifierModificationAutorisee();
        if (nouvelleDateDebut != null) this.dateDebut = nouvelleDateDebut;
        if (nouvelleDateFin != null) this.dateFin = nouvelleDateFin;
        if (nouveauType != null && !nouveauType.isBlank()) this.type = nouveauType.trim().toUpperCase();
        if (nouveauCommentaire != null) this.commentaire = nouveauCommentaire;
        validerDates();
        this.joursOuvres = calculerJoursOuvres();
        verifierSoldeSuffisant();
        this.urgente = ChronoUnit.DAYS.between(LocalDate.now(), this.dateDebut) < 7;
    }

    public void annuler() {
        if (!"EN_ATTENTE".equals(this.statut) && !"APPROUVE".equals(this.statut))
            throw new IllegalStateException("Seules les demandes EN_ATTENTE ou APPROUVÉES peuvent être annulées");
        if ("APPROUVE".equals(this.statut) && this.dateDebut.isBefore(LocalDate.now()))
            throw new IllegalStateException("Impossible d'annuler un congé déjà commencé");
        String ancien = this.statut;
        this.statut = "ANNULE";
        this.dateDecision = LocalDate.now();
        if ("APPROUVE".equals(ancien) && "ANNUEL".equals(this.type)
                && this.joursOuvres != null && this.employe != null) {
            this.employe.ajouterConges(this.joursOuvres);
        }
    }

    public void valider() {
        verifierValidationAutorisee();
        this.statut = "APPROUVE";
        this.dateDecision = LocalDate.now();
        if ("ANNUEL".equals(this.type) && this.joursOuvres != null && this.employe != null)
            this.employe.deduireConges(this.joursOuvres);
    }

    public void refuser() {
        verifierValidationAutorisee();
        this.statut = "REFUSE";
        this.dateDecision = LocalDate.now();
    }

    public void refuserAvecMotif(String motif) {
        refuser();
        this.motifRefus = motif;
    }

    public void notifierDecision() {
        String msg = String.format("Votre demande du %s au %s a été %s",
                this.dateDebut, this.dateFin,
                "APPROUVE".equals(this.statut) ? "approuvée" : "refusée");
        if (this.motifRefus != null) msg += " — Motif : " + this.motifRefus;
        log.info("NOTIFICATION : {}", msg);
    }

    // ✅ Getters/Setters explicites pour éviter les problèmes Lombok
    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }
    public String getCurrentTaskId() { return currentTaskId; }
    public void setCurrentTaskId(String currentTaskId) { this.currentTaskId = currentTaskId; }

    private void validerDates() {
        if (this.dateDebut == null || this.dateFin == null)
            throw new IllegalStateException("Les dates sont obligatoires");
        if (this.dateDebut.isAfter(this.dateFin))
            throw new IllegalStateException("Date début > date fin");
        if (this.dateDebut.isBefore(LocalDate.now()))
            throw new IllegalStateException("Date début dans le passé");
        if (ChronoUnit.MONTHS.between(this.dateDebut, this.dateFin) > 1)
            throw new IllegalStateException("Durée maximale dépassée (1 mois)");
    }

    private void validerType() {
        if (this.type == null || this.type.isBlank())
            throw new IllegalStateException("Type de congé obligatoire");
        this.type = this.type.toUpperCase();
    }

    private void verifierSoldeSuffisant() {
        if ("ANNUEL".equals(this.type) && this.employe != null && this.joursOuvres != null) {
            int dispo = this.employe.getSoldeConges();
            if (dispo < this.joursOuvres)
                throw new IllegalStateException(
                        String.format("Solde insuffisant — Disponible : %d, Demandé : %d", dispo, this.joursOuvres));
        }
    }

    private void verifierModificationAutorisee() {
        if (!"EN_ATTENTE".equals(this.statut))
            throw new IllegalStateException("Seules les demandes EN_ATTENTE peuvent être modifiées");
    }

    private void verifierValidationAutorisee() {
        if (!"EN_ATTENTE".equals(this.statut))
            throw new IllegalStateException("Seules les demandes EN_ATTENTE peuvent être validées/refusées");
    }

    private Integer calculerJoursOuvres() {
        if (this.dateDebut == null || this.dateFin == null) return 0;
        int count = 0;
        LocalDate cur = this.dateDebut;
        while (!cur.isAfter(this.dateFin)) {
            if (!WEEKEND.contains(cur.getDayOfWeek()) && !JOURS_FERIES.contains(cur)) count++;
            cur = cur.plusDays(1);
        }
        return count;
    }

    @Transient
    public long getNombreJoursCalendaires() {
        if (this.dateDebut == null || this.dateFin == null) return 0;
        return ChronoUnit.DAYS.between(this.dateDebut, this.dateFin) + 1;
    }

    @Transient public long getJoursAvantDebut() {
        return ChronoUnit.DAYS.between(LocalDate.now(), this.dateDebut);
    }

    @Transient public boolean estEnCours() {
        LocalDate aj = LocalDate.now();
        return "APPROUVE".equals(this.statut) && !aj.isBefore(this.dateDebut) && !aj.isAfter(this.dateFin);
    }

    @Transient public boolean estTermine() {
        return "APPROUVE".equals(this.statut) && LocalDate.now().isAfter(this.dateFin);
    }

    public boolean chevauche(DemandeConge autre) {
        if (autre == null) return false;
        return !(this.dateFin.isBefore(autre.dateDebut) || this.dateDebut.isAfter(autre.dateFin));
    }

    @Transient public boolean isValide() {
        return this.dateDebut != null && this.dateFin != null
                && !this.dateDebut.isAfter(this.dateFin)
                && this.type != null && !this.type.isBlank();
    }

    @Transient public String getResume() {
        return String.format("%s: %s du %s au %s (%d j) — %s%s",
                this.type,
                this.employe != null ? this.employe.getMatricule() : "???",
                this.dateDebut, this.dateFin,
                getNombreJoursCalendaires(),
                this.statut,
                Boolean.TRUE.equals(this.urgente) ? " [URGENT]" : "");
    }

    @PrePersist
    protected void onCreate() {
        initialiserValeursParDefaut();
        validerDates();
        validerType();
    }

    @PreUpdate
    protected void onUpdate() {
        initialiserValeursParDefaut();
        validerDates();
    }

    private void initialiserValeursParDefaut() {
        if (this.dateDemande == null) this.dateDemande = LocalDate.now();
        if (this.statut == null || this.statut.isBlank()) this.statut = "EN_ATTENTE";
        else this.statut = this.statut.toUpperCase();
        if (this.type != null) this.type = this.type.toUpperCase();
        if (this.joursOuvres == null) this.joursOuvres = calculerJoursOuvres();
        if (this.urgente == null)
            this.urgente = ChronoUnit.DAYS.between(LocalDate.now(), this.dateDebut) < 7;
    }
}