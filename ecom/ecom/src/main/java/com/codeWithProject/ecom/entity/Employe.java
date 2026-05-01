package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Employe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String password;
    private LocalDate dateEmbauche;
    private String poste;
    private Double salaire;
    private String adresse;
    private String statut = "ACTIF";
    private String photoUrl;
    private String departement;

    private Integer soldeConges = 25;
    private Boolean actif = true;

    private LocalDate dateCreation;
    private LocalDateTime derniereConnexion;

    private Integer nombreConnexions = 0;
    private Integer tentativesEchec = 0;
    private Boolean compteVerrouille = false;
    private LocalDateTime dateVerrouillage;

    private String role;

    // ===== RELATIONS =====

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manager_id")
    @Fetch(FetchMode.SELECT)
    @JsonIgnoreProperties({"employesGeres"})
    private Employe manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @OneToMany(mappedBy = "employe", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DemandeConge> demandesConge = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "employe_competence",
        joinColumns = @JoinColumn(name = "employe_id"),
        inverseJoinColumns = @JoinColumn(name = "competence_id")
    )
    @JsonIgnore
    private List<Competence> competences = new ArrayList<>();

    @OneToMany(mappedBy = "employe")
    @JsonIgnore
    private List<EmployeFormation> employeFormations;

    // ===== MÉTHODES MÉTIER =====

    public boolean hasRole(String roleName) {
        return role != null && role.equalsIgnoreCase(roleName);
    }

    public boolean isManager() {
        return "manager".equalsIgnoreCase(role);
    }

    public boolean isAdminRH() {
        return "ADMIN_RH".equalsIgnoreCase(role);
    }

    public boolean isEmploye() {
        return "user".equalsIgnoreCase(role);
    }

    public boolean seConnecter(String email, String password) {
        if (!this.actif) throw new IllegalStateException("Compte désactivé");
        if (this.compteVerrouille) throw new IllegalStateException("Compte verrouillé");

        if (!this.email.equalsIgnoreCase(email)) {
            enregistrerEchecConnexion();
            return false;
        }

        enregistrerConnexionReussie();
        return true;
    }

    private void enregistrerConnexionReussie() {
        this.derniereConnexion = LocalDateTime.now();
        this.nombreConnexions++;
        this.tentativesEchec = 0;
    }

    private void enregistrerEchecConnexion() {
        this.tentativesEchec++;
        if (this.tentativesEchec >= 5) verrouiller();
    }

    public void verrouiller() {
        this.compteVerrouille = true;
        this.dateVerrouillage = LocalDateTime.now();
    }

    public void deverrouiller() {
        this.compteVerrouille = false;
        this.tentativesEchec = 0;
    }

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }

    public void ajouterConges(int jours) {
        this.soldeConges += jours;
    }

    public void deduireConges(int jours) {
        if (this.soldeConges < jours) throw new IllegalStateException("Solde insuffisant");
        this.soldeConges -= jours;
    }

    public void soumettreDemandeConge(DemandeConge demande) {
        this.demandesConge.add(demande);
        demande.setEmploye(this);
    }

    @Transient
    public String getNomComplet() {
        return prenom + " " + nom;
    }

    @Transient
    public Integer getSoldeCongesSafe() {
        return soldeConges != null ? soldeConges : 0;
    }

    @Transient
    public long getAnciennete() {
        if (dateEmbauche == null) return 0;
        return ChronoUnit.YEARS.between(dateEmbauche, LocalDate.now());
    }

    @Transient
    public String getStatutCompte() {
        if (Boolean.TRUE.equals(compteVerrouille)) return "🔒 Verrouillé";
        if (!Boolean.TRUE.equals(actif)) return "❌ Inactif";
        return "✅ Actif";
    }

    @Transient
    public Double getSalaireAnnuel() {
        return salaire == null ? 0.0 : salaire * 12;
    }

    // ===== HOOKS =====

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        if (dateCreation == null) dateCreation = LocalDate.now();
        if (email != null) email = email.trim().toLowerCase();
    }
}