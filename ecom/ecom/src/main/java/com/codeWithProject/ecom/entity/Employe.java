package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employes",
        indexes = {
                @Index(name = "idx_employe_email", columnList = "email"),
                @Index(name = "idx_employe_actif", columnList = "actif"),
                @Index(name = "idx_employe_matricule", columnList = "matricule"),
                @Index(name = "idx_employe_statut", columnList = "statut"),
                @Index(name = "idx_employe_role", columnList = "role")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employe_email", columnNames = "email"),
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




    // ===== CONSTANTES POUR LES TYPES =====
    public static final String TYPE_EMPLOYE = "EMPLOYE";
    public static final String TYPE_MANAGER = "MANAGER";
    public static final String TYPE_ADMIN_RH = "ADMIN_RH";

    // ===== RELATIONS =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    @ToString.Exclude
    @JsonIgnore
    private Manager manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    @ToString.Exclude
    private Service service;

    @OneToMany(mappedBy = "employe", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<EmployeCompetence> competences = new ArrayList<>();
    @OneToMany(mappedBy = "employe", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    @Builder.Default
    private List<DemandeConge> demandesConge = new ArrayList<>();

    // ===== MÉTHODES POUR LE ROLE =====
    public boolean hasRole(String roleName) {
        return role != null && role.equals(roleName);
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

    // ===== MÉTHODES MÉTIER =====
    public boolean seConnecter(String email, String password) {
        if (!this.actif) {
            throw new IllegalStateException("Compte employé désactivé. Contactez l'administrateur.");
        }
        if (this.compteVerrouille) {
            throw new IllegalStateException(
                    String.format("Compte verrouillé depuis le %s. Contactez l'administrateur.",
                            this.dateVerrouillage)
            );
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
        this.nombreConnexions = (this.nombreConnexions == null ? 0 : this.nombreConnexions) + 1;
        this.tentativesEchec = 0;
    }

    private void enregistrerEchecConnexion() {
        this.tentativesEchec = (this.tentativesEchec == null ? 0 : this.tentativesEchec) + 1;
        if (this.tentativesEchec >= 5) {
            verrouiller();
        }
    }

    public void seDeconnecter() {
        // Géré par Spring Security
    }

    public void verrouiller() {
        this.compteVerrouille = true;
        this.dateVerrouillage = LocalDateTime.now();
    }

    public void deverrouiller() {
        this.compteVerrouille = false;
        this.dateVerrouillage = null;
        this.tentativesEchec = 0;
    }

    public void activer() {
        this.actif = true;
        if (Boolean.TRUE.equals(this.compteVerrouille)) {
            deverrouiller();
        }
    }

    public void desactiver() {
        this.actif = false;
    }

    public void mettreAJourInformations(String nom, String prenom, String telephone) {
        if (nom != null && !nom.trim().isEmpty()) {
            this.nom = nom.trim();
        }
        if (prenom != null && !prenom.trim().isEmpty()) {
            this.prenom = prenom.trim();
        }
        if (telephone != null) {
            this.telephone = telephone.trim();
        }
    }

    public void changerEmail(String nouvelEmail) {
        if (nouvelEmail == null || nouvelEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email ne peut pas être vide");
        }
        if (!nouvelEmail.contains("@") || !nouvelEmail.contains(".")) {
            throw new IllegalArgumentException("Format d'email invalide");
        }
        this.email = nouvelEmail.trim().toLowerCase();
    }

    public void ajouterConges(int jours) {
        this.soldeConges = getSoldeConges() + jours;
    }

    public void deduireConges(int jours) {
        int soldeActuel = getSoldeConges();
        if (soldeActuel < jours) {
            throw new IllegalStateException(
                    String.format("Solde de congés insuffisant. Disponible: %d, Demandé: %d",
                            soldeActuel, jours)
            );
        }
        this.soldeConges = soldeActuel - jours;
    }

    public void mettreAJourProfil(String poste, Double salaire, String departement) {
        if (poste != null) this.poste = poste;
        if (salaire != null) this.salaire = salaire;
        if (departement != null) this.departement = departement;
    }

    public void soumettreDemandeConge(DemandeConge demande) {
        if (this.demandesConge == null) {
            this.demandesConge = new ArrayList<>();
        }
        this.demandesConge.add(demande);
        demande.setEmploye(this);
    }

    public void annulerDemandeConge(DemandeConge demande) {
        demande.setStatut("ANNULE");
    }

    public List<DemandeConge> consulterDemandesConge(String statut) {
        if (this.demandesConge == null) {
            return new ArrayList<>();
        }
        return this.demandesConge.stream()
                .filter(d -> d.getStatut().equals(statut))
                .toList();
    }

    @Transient
    public String getNomComplet() {
        return this.prenom + " " + this.nom;
    }

    public String consulterProfil() {
        StringBuilder profil = new StringBuilder();
        profil.append("=== PROFIL EMPLOYÉ ===\n");
        profil.append("Matricule: ").append(this.matricule).append("\n");
        profil.append("Nom complet: ").append(getNomComplet()).append("\n");
        profil.append("Email: ").append(this.email).append("\n");
        profil.append("Rôle: ").append(this.role != null ? this.role : "Non défini").append("\n");
        profil.append("Poste: ").append(this.poste).append("\n");
        profil.append("Département: ").append(this.departement).append("\n");
        profil.append("Téléphone: ").append(this.telephone != null ? this.telephone : "Non renseigné").append("\n");
        profil.append("Statut: ").append(getStatutCompte()).append("\n");
        profil.append("Date d'embauche: ").append(this.dateEmbauche).append("\n");
        profil.append("Ancienneté: ").append(getAnciennete()).append(" ans\n");
        profil.append("Solde congés: ").append(getSoldeConges()).append(" jours\n");
        profil.append("Nombre de connexions: ").append(this.nombreConnexions != null ? this.nombreConnexions : 0).append("\n");
        if (this.derniereConnexion != null) {
            profil.append("Dernière connexion: ").append(this.derniereConnexion).append("\n");
        }
        return profil.toString();
    }

    @Transient
    public String getStatutCompte() {
        if (Boolean.TRUE.equals(this.compteVerrouille)) {
            return "🔒 Verrouillé";
        }
        if (!Boolean.TRUE.equals(this.actif)) {
            return "❌ Inactif";
        }
        return "✅ Actif";
    }

    @Transient
    public boolean peutSeConnecter() {
        return Boolean.TRUE.equals(this.actif) && !Boolean.TRUE.equals(this.compteVerrouille);
    }

    @Transient
    public long getJoursDepuisDerniereConnexion() {
        if (this.derniereConnexion == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(
                this.derniereConnexion.toLocalDate(),
                LocalDate.now()
        );
    }

    @Transient
    public boolean isCompteInactifLongtemps() {
        long jours = getJoursDepuisDerniereConnexion();
        return jours > 90;
    }

    @Transient
    public long getAnciennete() {
        if (this.dateEmbauche == null) return 0;
        return ChronoUnit.YEARS.between(this.dateEmbauche, LocalDate.now());
    }

    @Transient
    public Double getSalaireAnnuel() {
        return this.salaire != null ? this.salaire * 12 : 0.0;
    }

    @Transient
    public Integer getSoldeConges() {
        return soldeConges != null ? soldeConges : 0;
    }

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        if (this.nom == null || this.nom.trim().isEmpty()) {
            throw new IllegalStateException("Le nom est obligatoire");
        }
        if (this.prenom == null || this.prenom.trim().isEmpty()) {
            throw new IllegalStateException("Le prénom est obligatoire");
        }
        if (this.email == null || this.email.trim().isEmpty()) {
            throw new IllegalStateException("L'email est obligatoire");
        }
        if (this.matricule == null || this.matricule.trim().isEmpty()) {
            throw new IllegalStateException("Le matricule est obligatoire");
        }
        if (this.password == null || this.password.isEmpty()) {
            throw new IllegalStateException("Le mot de passe est obligatoire");
        }
        if (!this.email.contains("@")) {
            throw new IllegalStateException("Format d'email invalide");
        }

        if (this.dateCreation == null) {
            this.dateCreation = LocalDate.now();
        }
        if (this.actif == null) {
            this.actif = true;
        }
        if (this.nombreConnexions == null) {
            this.nombreConnexions = 0;
        }
        if (this.tentativesEchec == null) {
            this.tentativesEchec = 0;
        }
        if (this.compteVerrouille == null) {
            this.compteVerrouille = false;
        }
        if (this.statut == null) {
            this.statut = "ACTIF";
        }
        if (this.soldeConges == null) {
            this.soldeConges = 25;
        }

        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
        if (this.matricule != null) {
            this.matricule = this.matricule.trim().toUpperCase();
        }
        if (this.nom != null) {
            this.nom = this.nom.trim().toUpperCase();
        }
        if (this.prenom != null) {
            String p = this.prenom.trim();
            this.prenom = p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase();
        }
    }

@ManyToMany
@JoinTable(
    name = "employe_competence",
    joinColumns = @JoinColumn(name = "employe_id"),
    inverseJoinColumns = @JoinColumn(name = "competence_id")
)
private List<Competence> competence;




}