package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entité SystemeBI - Représente le système de Business Intelligence
 *
 * Gère les indicateurs RH, les analyses et les prédictions
 *
 * Relations:
 * - OneToMany avec IndicateurRH (indicateurs calculés)
 * - OneToMany avec ScoreTurnover (scores prédictifs)
 */
@Entity
@Table(name = "systemes_bi")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemeBI {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String version;

    @Column(name = "outil_etl", length = 150)
    private String outilETL; // Talend, Pentaho, Python, Airflow, etc.

    @Column(name = "outil_visualisation", length = 150)
    private String outilVisualisation; // Power BI, Tableau, Superset, etc.

    @Column(name = "modele_ml", length = 150)
    private String modeleML; // RandomForest, XGBoost, etc.

    @Column(name = "derniere_execution")
    private java.time.LocalDateTime derniereExecution;

    @Column(name = "statut", length = 50)
    @Builder.Default
    private String statut = "ACTIF"; // ACTIF, MAINTENANCE, INACTIF

    // ===== RELATIONS =====

    /**
     * Liste des indicateurs RH calculés par le système
     * Relation OneToMany avec IndicateurRH
     */
    @OneToMany(mappedBy = "systemeBI", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<IndicateurRH> indicateurs = new ArrayList<>();

    /**
     * Liste des scores de turnover calculés
     * Relation OneToMany avec ScoreTurnover
     */
    @OneToMany(mappedBy = "systemeBI", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<ScoreTurnover> scoresTurnover = new ArrayList<>();

    // ===== MÉTHODES MÉTIER - ETL =====

    /**
     * Exécute le processus ETL (Extract, Transform, Load)
     */
    public void executerETL() {
        if (!"ACTIF".equals(this.statut)) {
            throw new IllegalStateException("Le système BI n'est pas actif");
        }

        // Logique d'exécution ETL
        // 1. Extraction des données depuis la base transactionnelle
        // 2. Transformation des données (nettoyage, agrégation)
        // 3. Chargement dans le data warehouse

        this.derniereExecution = java.time.LocalDateTime.now();
    }

    /**
     * Planifie l'exécution automatique de l'ETL
     */
    public void planifierETL(String frequence) {
        // Logique de planification (cron job, scheduler, etc.)
        // Sera implémentée avec Spring Scheduler ou Quartz
    }

    // ===== MÉTHODES MÉTIER - ANALYSE DES COMPÉTENCES =====

    /**
     * Analyse les compétences de l'entreprise
     */
    public void analyserCompetences() {
        // Logique d'analyse:
        // - Identification des compétences les plus fréquentes
        // - Détection des gaps de compétences
        // - Analyse des niveaux de compétence par département
    }

    /**
     * Recommande des formations basées sur les compétences manquantes
     */
    public List<Formation> recommanderFormations(Employe employe) {
        // Logique de recommandation:
        // - Analyse des compétences de l'employé
        // - Identification des compétences manquantes pour son poste
        // - Recommandation de formations pertinentes

        return new ArrayList<>(); // À implémenter
    }

    // ===== MÉTHODES MÉTIER - ANALYSE DU TURNOVER =====

    /**
     * Analyse le turnover de l'entreprise
     */
    public IndicateurRH analyserTurnover() {
        // Logique de calcul du turnover:
        // Taux de turnover = (Nombre de départs / Effectif moyen) * 100

        IndicateurRH indicateur = IndicateurRH.builder()
                .type("TURNOVER")
                .periode("ANNUEL")
                .systemeBI(this)
                .build();

        indicateur.calculerIndicateur();
        this.indicateurs.add(indicateur);

        return indicateur;
    }

    /**
     * Calcule le taux de turnover pour une période donnée
     */
    public double calculerTauxTurnover(java.time.LocalDate dateDebut, java.time.LocalDate dateFin) {
        // Logique de calcul
        // À implémenter avec requêtes vers la base de données
        return 0.0;
    }

    // ===== MÉTHODES MÉTIER - ANALYSE DE L'ABSENTÉISME =====

    /**
     * Analyse l'absentéisme dans l'entreprise
     */
    public IndicateurRH analyserAbsenteisme() {
        // Logique de calcul de l'absentéisme:
        // Taux d'absentéisme = (Nombre de jours d'absence / Nombre de jours travaillés théoriques) * 100

        IndicateurRH indicateur = IndicateurRH.builder()
                .type("ABSENTEISME")
                .periode("MENSUEL")
                .systemeBI(this)
                .build();

        indicateur.calculerIndicateur();
        this.indicateurs.add(indicateur);

        return indicateur;
    }

    /**
     * Identifie les patterns d'absentéisme
     */
    public void identifierPatternsAbsenteisme() {
        // Logique d'analyse:
        // - Identification des jours de la semaine avec le plus d'absences
        // - Identification des périodes de l'année critiques
        // - Analyse par département
    }

    // ===== MÉTHODES MÉTIER - PRÉDICTION =====

    /**
     * Prédit le risque de turnover pour tous les employés
     */
    public void predireTurnover() {
        if (this.modeleML == null) {
            throw new IllegalStateException("Aucun modèle ML configuré");
        }

        // Logique de prédiction avec modèle ML:
        // 1. Récupération des features des employés
        // 2. Prédiction avec le modèle ML
        // 3. Création des scores de turnover

        this.derniereExecution = java.time.LocalDateTime.now();
    }

    /**
     * Prédit le risque de turnover pour un employé spécifique
     */
    public ScoreTurnover predireTurnoverEmploye(Employe employe) {
        if (employe == null) {
            throw new IllegalArgumentException("L'employé ne peut pas être nul");
        }

        // Logique de prédiction
        // Sera implémentée avec un modèle ML (scikit-learn, TensorFlow, etc.)

        ScoreTurnover score = ScoreTurnover.builder()
                .employe(employe)
                .systemeBI(this)
                .build();

        score.calculerScore();
        this.scoresTurnover.add(score);

        return score;
    }

    /**
     * Entraine le modèle ML de prédiction de turnover
     */
    public void entrainerModele() {
        // Logique d'entrainement du modèle:
        // 1. Préparation des données historiques
        // 2. Feature engineering
        // 3. Entrainement du modèle
        // 4. Validation et évaluation
        // 5. Sauvegarde du modèle
    }

    // ===== MÉTHODES MÉTIER - DASHBOARDS =====

    /**
     * Génère un dashboard de synthèse RH
     */
    public void genererDashboardSynthese() {
        // Logique de génération du dashboard:
        // - KPIs principaux (effectif, turnover, absentéisme)
        // - Graphiques de tendances
        // - Alertes et notifications
    }

    /**
     * Génère un rapport d'analyse des compétences
     */
    public void genererRapportCompetences() {
        // Logique de génération du rapport:
        // - Cartographie des compétences
        // - Identification des gaps
        // - Recommandations de formations
    }

    // ===== MÉTHODES UTILITAIRES =====

    /**
     * Obtient le nombre total d'indicateurs
     */
    public int getNombreIndicateurs() {
        return this.indicateurs != null ? this.indicateurs.size() : 0;
    }

    /**
     * Obtient le nombre total de scores de turnover
     */
    public int getNombreScoresTurnover() {
        return this.scoresTurnover != null ? this.scoresTurnover.size() : 0;
    }

    /**
     * Vérifie si le système est opérationnel
     */
    @Transient
    public boolean isOperationnel() {
        return "ACTIF".equals(this.statut);
    }

    /**
     * Active le système BI
     */
    public void activer() {
        this.statut = "ACTIF";
    }

    /**
     * Met le système en maintenance
     */
    public void mettreEnMaintenance() {
        this.statut = "MAINTENANCE";
    }

    /**
     * Désactive le système BI
     */
    public void desactiver() {
        this.statut = "INACTIF";
    }

    /**
     * Initialisation lors de la création
     */
    @PrePersist
    protected void onCreate() {
        if (this.statut == null) {
            this.statut = "ACTIF";
        }
    }
}