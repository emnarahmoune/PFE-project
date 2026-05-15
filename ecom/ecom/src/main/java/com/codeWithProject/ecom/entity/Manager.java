package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "managers")
@DiscriminatorValue("MANAGER") 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Manager extends Employe {

    // =========================
    // 🔥 IMPORTANT : NE PAS REDÉCLARER typeEmploye ICI
    // =========================

    @Column(name = "date_nomination")
    private LocalDate dateNomination;

    // ================= RELATIONS =================

    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Employe> employesGeres = new ArrayList<>();

    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnoreProperties({"employe", "manager"})
    @Builder.Default
    private List<DemandeConge> demandesCongeAValider = new ArrayList<>();

    // ================= MÉTHODES =================

    public void ajouterEmploye(Employe employe) {
        if (employe == null) return;

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

    public List<Employe> consulterEmployesActifs() {
        return employesGeres.stream()
                .filter(e -> "ACTIF".equals(e.getStatut()))
                .collect(Collectors.toList());
    }

    @Transient
    public int getNombreEmployesGeres() {
        return consulterEmployesActifs().size();
    }

    @Transient
    public long getNombreDemandesEnAttente() {
        return demandesCongeAValider.stream()
                .filter(d -> "EN_ATTENTE".equals(d.getStatut()))
                .count();
    }

    @Transient
    public long getAncienneteManager() {
        if (dateNomination == null) return 0;
        return ChronoUnit.MONTHS.between(dateNomination, LocalDate.now());
    }

    @PostLoad
    @PostPersist
    private void initLists() {
        if (employesGeres == null) employesGeres = new ArrayList<>();
        if (demandesCongeAValider == null) demandesCongeAValider = new ArrayList<>();
    }


    @Transient
    public int getNombreEmployesTotal() {
        return employesGeres != null ? employesGeres.size() : 0;
    }


    @Transient
    public List<DemandeConge> getDemandesUrgentes() {
        if (demandesCongeAValider == null) return new ArrayList<>();

        LocalDate dateLimite = LocalDate.now().plusDays(7);

        return demandesCongeAValider.stream()
                .filter(d -> "EN_ATTENTE".equals(d.getStatut())
                        && d.getDateDebut() != null
                        && d.getDateDebut().isBefore(dateLimite))
                .collect(Collectors.toList());
    }

    @Transient
    public String genererRapportEquipe() {
        StringBuilder rapport = new StringBuilder();

        rapport.append("=== RAPPORT D'ÉQUIPE ===\n");
        rapport.append("Manager: ").append(getPrenom()).append(" ").append(getNom()).append("\n");
        rapport.append("Département: ").append(getDepartement()).append("\n");
        rapport.append("Effectif total: ").append(getNombreEmployesTotal()).append("\n");
        rapport.append("Demandes en attente: ").append(getNombreDemandesEnAttente()).append("\n");

        long urgentes = getDemandesUrgentes().size();
        if (urgentes > 0) {
            rapport.append("⚠️ Demandes urgentes: ").append(urgentes).append("\n");
        }

        return rapport.toString();
    }



    @Transient
public boolean peutSeConnecter() {
    return Boolean.TRUE.equals(this.getActif())
            && !Boolean.TRUE.equals(this.getCompteVerrouille());
}

public void activer() {
    this.setActif(true);
    this.setCompteVerrouille(false);
    this.setStatut("ACTIF");
}

public void desactiver() {
    this.setActif(false);
    this.setStatut("INACTIF");
}
}