package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "utilisateurs",
        indexes = {
                @Index(name = "idx_utilisateur_email", columnList = "email"),
                @Index(name = "idx_utilisateur_actif", columnList = "actif")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_utilisateur_email", columnNames = "email")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Utilisateur {  // ← Plus abstract !

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "password")
    private String password;

    @Column(name = "actif", nullable = false)
    private Boolean actif;

    @Column(name = "date_creation")
    private LocalDate dateCreation;

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    @Column(name = "nombre_connexions")
    private Integer nombreConnexions;

    @Column(name = "tentatives_echec")
    private Integer tentativesEchec;

    @Column(name = "compte_verrouille", nullable = false)
    private Boolean compteVerrouille;

    @Column(name = "date_verrouillage")
    private LocalDateTime dateVerrouillage;

    @Column(name = "type_utilisateur", insertable = false, updatable = false)
    private String typeUtilisateur;

    // ===== RELATION AVEC EMPLOYE =====
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", unique = true)
    @ToString.Exclude
    private Employe employe;

    // ===== MÉTHODES MÉTIER =====
    public boolean seConnecter(String email, String password) {
        if (!this.actif) {
            throw new IllegalStateException("Compte utilisateur désactivé. Contactez l'administrateur.");
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

    @Transient
    public String getNomComplet() {
        return this.prenom + " " + this.nom;
    }

    public String consulterProfil() {
        StringBuilder profil = new StringBuilder();
        profil.append("=== PROFIL UTILISATEUR ===\n");
        profil.append("Nom complet: ").append(getNomComplet()).append("\n");
        profil.append("Email: ").append(this.email).append("\n");
        profil.append("Téléphone: ").append(this.telephone != null ? this.telephone : "Non renseigné").append("\n");
        profil.append("Statut: ").append(getStatutCompte()).append("\n");
        profil.append("Date de création: ").append(this.dateCreation).append("\n");
        profil.append("Nombre de connexions: ").append(this.nombreConnexions != null ? this.nombreConnexions : 0).append("\n");

        if (this.derniereConnexion != null) {
            profil.append("Dernière connexion: ").append(this.derniereConnexion).append("\n");
        }

        if (this.employe != null) {
            profil.append("\n=== INFORMATIONS EMPLOYÉ ===\n");
            profil.append("Matricule: ").append(this.employe.getMatricule()).append("\n");
            profil.append("Poste: ").append(this.employe.getPoste()).append("\n");
            profil.append("Département: ").append(this.employe.getDepartement()).append("\n");
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

    // ===== LIFECYCLE CALLBACKS =====
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

        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
        if (this.nom != null) {
            this.nom = this.nom.trim().toUpperCase();
        }
        if (this.prenom != null) {
            String p = this.prenom.trim();
            this.prenom = p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase();
        }
    }
}