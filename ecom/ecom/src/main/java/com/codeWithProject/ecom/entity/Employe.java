package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employes",
        indexes = {
                @Index(name = "idx_employe_matricule", columnList = "matricule"),
                @Index(name = "idx_employe_statut", columnList = "statut"),
                @Index(name = "idx_employe_dept", columnList = "departement")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    /* ── Relations ─────────────────────────────────────────── */

    @OneToOne(mappedBy = "employe", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    @ToString.Exclude
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    @ToString.Exclude
    private Manager manager;

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
    @Builder.Default
    private List<DemandeConge> demandesConge = new ArrayList<>();

    /* ── Méthodes métier ────────────────────────────────────── */

    /**
     * Crée et associe un Utilisateur pour cet employé
     */
    public Utilisateur createUtilisateur(String encodedPassword) {
        if (this.utilisateur != null) {
            return this.utilisateur;
        }

        // Créer une instance concrète de Utilisateur (plus abstract)
        Utilisateur newUtilisateur = new Utilisateur();
        newUtilisateur.setNom(this.nom);
        newUtilisateur.setPrenom(this.prenom);
        newUtilisateur.setEmail(this.email);
        newUtilisateur.setTelephone(this.telephone);
        newUtilisateur.setPassword(encodedPassword);
        newUtilisateur.setActif(true);
        newUtilisateur.setCompteVerrouille(false);
        newUtilisateur.setNombreConnexions(0);
        newUtilisateur.setTentativesEchec(0);
        newUtilisateur.setEmploye(this);

        this.utilisateur = newUtilisateur;
        return newUtilisateur;
    }

    public Integer getSoldeConges() {
        return soldeConges != null ? soldeConges : 0;
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

        if (this.utilisateur != null) {
            this.utilisateur.mettreAJourInformations(this.nom, this.prenom, this.telephone);
        }
    }

    public List<DemandeConge> consulterConges() {
        return new ArrayList<>(this.demandesConge);
    }

    public void soumettreDemandeConge(DemandeConge demande) {
        this.demandesConge.add(demande);
        demande.setEmploye(this);
    }

    public void annulerDemandeConge(DemandeConge demande) {
        demande.setStatut("ANNULE");
    }

    public List<DemandeConge> consulterDemandesConge(String statut) {
        return this.demandesConge.stream()
                .filter(d -> d.getStatut().equals(statut))
                .toList();
    }

    @Transient
    public long getAnciennete() {
        if (this.dateEmbauche == null) return 0;
        return java.time.temporal.ChronoUnit.YEARS.between(this.dateEmbauche, LocalDate.now());
    }

    @Transient
    public Double getSalaireAnnuel() {
        return this.salaire != null ? this.salaire * 12 : 0.0;
    }

    @Transient
    public int getNombreFormations() {
        return this.formations != null ? this.formations.size() : 0;
    }

    @Transient
    public int getNombreDemandesConge() {
        return this.demandesConge != null ? this.demandesConge.size() : 0;
    }

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        if (this.statut == null) this.statut = "ACTIF";
        if (this.soldeConges == null) this.soldeConges = 25;
        if (this.matricule != null) this.matricule = this.matricule.trim().toUpperCase();
        if (this.email != null) this.email = this.email.trim().toLowerCase();
        if (this.nom != null) this.nom = this.nom.trim().toUpperCase();
        if (this.prenom != null) {
            String p = this.prenom.trim();
            this.prenom = p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase();
        }
    }
}