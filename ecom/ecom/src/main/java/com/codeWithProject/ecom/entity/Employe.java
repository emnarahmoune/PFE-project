package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "employes")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_employe", discriminatorType = DiscriminatorType.STRING)
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
    private Double salaire;


    @Column(name = "adresse")
    private String adresse;

    @Builder.Default
    private String statut = "ACTIF";

    private String departement;

    @Builder.Default
    private Integer soldeConges = 25;

    @Builder.Default
    private Boolean actif = true;

    private LocalDate dateCreation;
    private LocalDateTime derniereConnexion;

    @Builder.Default
    private Integer nombreConnexions = 0;

    @Builder.Default
    private Integer tentativesEchec = 0;

    @Builder.Default
    private Boolean compteVerrouille = false;

    private LocalDateTime dateVerrouillage;

    private String role;

    // ===== CONSTANTES =====
    public static final String TYPE_EMPLOYE = "EMPLOYE";
    public static final String TYPE_MANAGER = "MANAGER";
    public static final String TYPE_ADMIN_RH = "ADMIN_RH";

    // ===== RELATIONS =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    @JsonIgnoreProperties({"manager"})
    private Employe manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @OneToMany(mappedBy = "employe", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<DemandeConge> demandesConge = new ArrayList<>();



    @Column(name = "type_employe", insertable = false, updatable = false)
private String typeEmploye;

    // ===== MÉTHODES =====

    public boolean isManager() {
        return "manager".equalsIgnoreCase(role);
    }

    public boolean isAdminRH() {
        return "ADMIN_RH".equalsIgnoreCase(role);
    }

    public boolean isEmploye() {
        return "user".equalsIgnoreCase(role);
    }

    public void verrouiller() {
        this.compteVerrouille = true;
        this.dateVerrouillage = LocalDateTime.now();
    }

    public void deverrouiller() {
        this.compteVerrouille = false;
        this.tentativesEchec = 0;
    }

    @Transient
    public String getNomComplet() {
        return prenom + " " + nom;
    }

    @Transient
    public long getAnciennete() {
        if (dateEmbauche == null) return 0;
        return ChronoUnit.YEARS.between(dateEmbauche, LocalDate.now());
    }

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        if (dateCreation == null) dateCreation = LocalDate.now();
        if (email != null) email = email.toLowerCase().trim();
    }


    @Transient
public Double getSalaireAnnuel() {
    if (salaire == null) return 0.0;
    return salaire * 12;
}



public String getAdresse() {
    return adresse;
}

public void setAdresse(String adresse) {
    this.adresse = adresse;
}




@ManyToMany
@JoinTable(
    name = "employe_competence",
    joinColumns = @JoinColumn(name = "employe_id"),
    inverseJoinColumns = @JoinColumn(name = "competence_id")
)
@JsonIgnore // 🔥 IMPORTANT
private List<Competence> competences;

@OneToMany(mappedBy = "employe")
@JsonIgnore
private List<EmployeFormation> employeFormations;

}