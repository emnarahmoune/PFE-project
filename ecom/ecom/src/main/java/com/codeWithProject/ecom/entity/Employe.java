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
@Table(name = "employes",
        indexes = {
                @Index(name = "idx_employe_email",     columnList = "email"),
                @Index(name = "idx_employe_actif",     columnList = "actif"),
                @Index(name = "idx_employe_matricule", columnList = "matricule"),
                @Index(name = "idx_employe_statut",    columnList = "statut"),
                @Index(name = "idx_employe_role",      columnList = "role")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employe_email",     columnNames = "email"),
                @UniqueConstraint(name = "uk_employe_matricule", columnNames = "matricule")
        }
)
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
    @Column(name = "id")
    private Long id;

    @Column(name = "matricule", nullable = false, unique = true, length = 50)
    private String matricule;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "date_embauche", nullable = false)
    private LocalDate dateEmbauche;

    @Column(name = "poste", length = 100)
    private String poste;

    @Column(name = "salaire", nullable = false)
    private Double salaire;

    @Column(name = "statut", length = 50, nullable = false)
    @Builder.Default
    private String statut = "ACTIF";

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Column(name = "departement", length = 100)
    private String departement;

    @Column(name = "solde_conges")
    @Builder.Default
    private Integer soldeConges = 25;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(name = "date_creation")
    private LocalDate dateCreation;

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    @Column(name = "nombre_connexions")
    @Builder.Default
    private Integer nombreConnexions = 0;

    @Column(name = "tentatives_echec")
    @Builder.Default
    private Integer tentativesEchec = 0;

    @Column(name = "compte_verrouille", nullable = false)
    @Builder.Default
    private Boolean compteVerrouille = false;

    @Column(name = "date_verrouillage")
    private LocalDateTime dateVerrouillage;

    @Column(name = "type_employe", insertable = false, updatable = false)
    private String typeEmploye;

    @Column(name = "role", length = 50)
    private String role;

    // ===== RELATIONS =====

    /**
     * CORRECTION HHH000179 : la relation vers le manager (qui peut être
     * un sous-type Manager) provoque un "narrowing proxy" quand Hibernate
     * charge un proxy Employe et découvre que c'est en réalité un Manager.
     *
     * Solution sans bytecode enhancement : FetchType.EAGER sur cette seule
     * relation ManyToOne. Le coût est acceptable car un employé a toujours
     * un seul manager, et la jointure est déjà présente dans les requêtes
     * générées (LEFT JOIN managers visible dans les logs).
     *
     * Alternative si EAGER est indésirable : ajouter le plugin
     * hibernate-enhance-maven-plugin dans pom.xml et utiliser
     * @LazyToOne(LazyToOneOption.NO_PROXY).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manager_id")
    @Fetch(FetchMode.SELECT)
    @ToString.Exclude
    @JsonIgnoreProperties({"employesGeres", "demandesCongeAValider", "manager", "competences",
            "formations", "demandesConge", "equipe"})
    private Employe manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    @ToString.Exclude
    private com.codeWithProject.ecom.entity.Service service;

    @OneToMany(mappedBy = "employe", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<EmployeCompetence> competences = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "employe_formation",
            joinColumns = @JoinColumn(name = "employe_id"),
            inverseJoinColumns = @JoinColumn(name = "formation_id")
    )
    @ToString.Exclude
    @Builder.Default
    private List<Formation> formations = new ArrayList<>();

    @OneToMany(mappedBy = "employe", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    @Builder.Default
    private List<DemandeConge> demandesConge = new ArrayList<>();

    // ===== CONSTANTES =====
    public static final String TYPE_EMPLOYE  = "EMPLOYE";
    public static final String TYPE_MANAGER  = "MANAGER";
    public static final String TYPE_ADMIN_RH = "ADMIN_RH";

    // ===== MÉTHODES MÉTIER =====

    public boolean hasRole(String roleName) {
        return role != null && role.equalsIgnoreCase(roleName);
    }

    public boolean isManager() {
        return "manager".equalsIgnoreCase(role);
    }

    public boolean isAdminRH() {
        return "ADMIN_RH".equals(role);
    }

    public boolean isEmploye() {
        return "user".equals(role);
    }

    public boolean seConnecter(String email, String password) {
        if (!this.actif) {
            throw new IllegalStateException("Compte employé désactivé. Contactez l'administrateur.");
        }
        if (this.compteVerrouille) {
            throw new IllegalStateException("Compte verrouillé. Contactez l'administrateur.");
        }
        if (!this.email.equalsIgnoreCase(email)) {
            enregistrerEchecConnexion();
            return false;
        }
        enregistrerConnexionReussie();
        return true;
    }

    private void enregistrerConnexionReussie() {
        this.derniereConnexion = LocalDateTime.now();
        this.nombreConnexions  = (this.nombreConnexions == null ? 0 : this.nombreConnexions) + 1;
        this.tentativesEchec   = 0;
    }

    private void enregistrerEchecConnexion() {
        this.tentativesEchec = (this.tentativesEchec == null ? 0 : this.tentativesEchec) + 1;
        if (this.tentativesEchec >= 5) {
            verrouiller();
        }
    }

    public void verrouiller() {
        this.compteVerrouille = true;
        this.dateVerrouillage = LocalDateTime.now();
    }

    public void deverrouiller() {
        this.compteVerrouille = false;
        this.dateVerrouillage = null;
        this.tentativesEchec  = 0;
    }

    public void activer() {
        this.actif = true;
        if (Boolean.TRUE.equals(this.compteVerrouille)) deverrouiller();
    }

    public void desactiver() {
        this.actif = false;
    }

    public void mettreAJourInformations(String nom, String prenom, String telephone) {
        if (nom != null       && !nom.trim().isEmpty())    this.nom       = nom.trim();
        if (prenom != null    && !prenom.trim().isEmpty()) this.prenom    = prenom.trim();
        if (telephone != null)                             this.telephone = telephone.trim();
    }

    public void ajouterConges(int jours) {
        this.soldeConges = getSoldeConges() + jours;
    }

    public void deduireConges(int jours) {
        int soldeActuel = getSoldeConges();
        if (soldeActuel < jours) throw new IllegalStateException("Solde insuffisant");
        this.soldeConges = soldeActuel - jours;
    }

    public void soumettreDemandeConge(DemandeConge demande) {
        if (this.demandesConge == null) this.demandesConge = new ArrayList<>();
        this.demandesConge.add(demande);
        demande.setEmploye(this);
    }

    @Transient
    public String getNomComplet() {
        return this.prenom + " " + this.nom;
    }

    @Transient
    public Integer getSoldeConges() {
        return soldeConges != null ? soldeConges : 0;
    }

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        if (this.dateCreation      == null)  this.dateCreation      = LocalDate.now();
        if (this.actif             == null)  this.actif             = true;
        if (this.nombreConnexions  == null)  this.nombreConnexions  = 0;
        if (this.tentativesEchec   == null)  this.tentativesEchec   = 0;
        if (this.compteVerrouille  == null)  this.compteVerrouille  = false;
        if (this.statut            == null)  this.statut            = "ACTIF";
        if (this.soldeConges       == null)  this.soldeConges       = 25;

        if (this.email     != null) this.email     = this.email.trim().toLowerCase();
        if (this.matricule != null) this.matricule = this.matricule.trim().toUpperCase();
        if (this.nom       != null) this.nom       = this.nom.trim().toUpperCase();
        if (this.prenom    != null) {
            String p    = this.prenom.trim();
            this.prenom = p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase();
        }
    }

    @Transient
    public long getAnciennete() {
        if (dateEmbauche == null) return 0;
        return ChronoUnit.YEARS.between(dateEmbauche, LocalDate.now());
    }

    @Transient
    public Double getSalaireAnnuel() {
        return salaire != null ? salaire * 12 : 0.0;
    }

    @Transient
    public String getStatutCompte() {
        if (Boolean.TRUE.equals(compteVerrouille)) return "🔒 Verrouillé";
        if (!Boolean.TRUE.equals(actif))           return "❌ Inactif";
        return "✅ Actif";
    }

    @Transient
    public boolean peutSeConnecter() {
        return Boolean.TRUE.equals(actif) && !Boolean.TRUE.equals(compteVerrouille);
    }
}