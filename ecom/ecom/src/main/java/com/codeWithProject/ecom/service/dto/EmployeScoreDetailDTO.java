package com.codeWithProject.ecom.service.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class EmployeScoreDetailDTO {
    private Long employeId;
    private String nom;
    private String prenom;
    private String matricule;
    private String poste;
    private String departement;
    private String dateEmbauche;
    private String anciennete;
    private Double salaireAnnuel;
    private String managerNom;
    private String photoUrl;
    private ScoreTurnoverDTO scoreActuel;
    private ScoreTurnoverDTO scorePrecedent;
    private Double evolutionScore;
    private Double evolutionPourcentage;
    private Integer rang;
    private Integer percentile;
    private Integer totalEmployes;
    private List<SousScoreItem> sousScores;
    private List<FacteurItem> facteursContributifs;
    private List<ActionItem> actionsRecommandees;
    private List<HistoriqueLigne> historique;
    private InfoCalcul infoCalcul;

    @Data @Builder public static class SousScoreItem {
        private String critere;
        private Double valeur;
        private Double max;
        private Integer contribution;
        private String couleur;
    }

    @Data @Builder public static class FacteurItem {
        private String libelle;
        private Double score;
        private String niveauImpact;
    }

    @Data @Builder public static class ActionItem {
        private String action;
        private String type;
    }

    @Data @Builder public static class HistoriqueLigne {
        private String date;
        private Double scoreGlobal;
        private String niveau;
        private String anciennete;
        private String salaire;
        private String performance;
        private String formations;
        private String absenteisme;
        private String facteursMajeurs;
    }

    @Data @Builder public static class InfoCalcul {
        private String periodeDebut;
        private String periodeFin;
        private String methode;
        private String source;
        private String dernierBatch;
        private String prochainBatch;
    }
}