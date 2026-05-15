package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employes")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_employe")
@DiscriminatorValue("EMPLOYE")
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
@Column(precision = 10, scale = 2)
private BigDecimal salaire;

private String adresse;
    private String statut = "ACTIF";
    private String departement;

    private Integer soldeConges = 25;
    private Boolean actif = true;

    private LocalDate dateCreation;
    private LocalDateTime derniereConnexion;

    private Integer nombreConnexions = 0;
    private Integer tentativesEchec = 0;
    private Boolean compteVerrouille = false;
    private LocalDateTime dateVerrouillage;


private String employePhotoProfil;
private String employePhotoUrl;
    private String role;

    // 🔥 CONSTANTES
    public static final String TYPE_ADMIN_RH = "ADMIN_RH";
    public static final String TYPE_MANAGER = "MANAGER";
    public static final String TYPE_EMPLOYE = "EMPLOYE";

    @Column(name = "photo_url")
    private String photoUrl;


public String getPhotoUrl() {
    return photoUrl;
}

public void setPhotoUrl(String photoUrl) {
    this.photoUrl = photoUrl;
}


public void ajouterConges(Integer jours) {
    if (jours == null || jours <= 0) {
        return;
    }

    if (this.soldeConges == null) {
        this.soldeConges = 0;
    }

    this.soldeConges += jours;
}

public void deduireConges(Integer jours) {
    if (jours == null || jours <= 0) {
        return;
    }

    if (this.soldeConges == null) {
        this.soldeConges = 0;
    }

    if (this.soldeConges < jours) {
        throw new IllegalStateException(
                "Solde insuffisant — Disponible : " + this.soldeConges + ", demandé : " + jours
        );
    }

    this.soldeConges -= jours;
}
 
    // =========================
    // RELATIONS
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "manager_id")
@JsonIgnoreProperties({
        "manager",
        "employesGeres",
        "demandesConge",
        "competences",
        "employeFormations",
        "service"
})
private Employe manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @OneToMany(
    mappedBy = "employe",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    targetEntity = DemandeConge.class
)
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

    // =========================
    // MÉTHODES MÉTIER
    // =========================

    public boolean hasRole(String roleName) {
        return role != null && role.equalsIgnoreCase(roleName);
    }

    public boolean isManager() {
        return "MANAGER".equalsIgnoreCase(role);
    }

    public boolean isAdminRH() {
        return "ADMIN_RH".equalsIgnoreCase(role);
    }

    public boolean isEmploye() {
        return "USER".equalsIgnoreCase(role);
    }

    // =========================
    // CONNEXION
    // =========================

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

    // =========================
    // UTILITAIRES
    // =========================

    @Transient
    public String getNomComplet() {
        return prenom + " " + nom;
    }

    @Transient
    public long getAnciennete() {
        if (dateEmbauche == null) return 0;
        return ChronoUnit.YEARS.between(dateEmbauche, LocalDate.now());
    }

    @Transient
public BigDecimal getSalaireAnnuel() {
    return salaire == null
            ? BigDecimal.ZERO
            : salaire.multiply(BigDecimal.valueOf(12));
}

    // =========================
    // HOOKS
    // =========================

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        if (dateCreation == null) dateCreation = LocalDate.now();
        if (email != null) email = email.trim().toLowerCase();
    }


    public LocalDate getDateEmbauche() {
    return dateEmbauche;
}

public void setDateEmbauche(LocalDate dateEmbauche) {
    this.dateEmbauche = dateEmbauche;
}


   
}