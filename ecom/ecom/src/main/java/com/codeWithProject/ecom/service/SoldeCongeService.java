package com.codeWithProject.ecom.service;

public interface SoldeCongeService {

    // ===== MÉTHODES EXISTANTES =====
    void augmenterSoldesMensuels();

    // ===== NOUVELLES MÉTHODES À AJOUTER =====

    /**
     * Récupère le solde total d'un employé
     */
    int getSoldeTotal(Long employeId);

    /**
     * Récupère les jours déjà pris dans l'année
     */
    int getJoursPrisDansAnnee(Long employeId, int annee);

    /**
     * Récupère le solde restant (total - pris)
     */
    int getSoldeRestant(Long employeId, int annee);

    /**
     * Vérifie si le solde est suffisant
     */
    boolean verifierSoldeSuffisant(Long employeId, int joursDemandes, int annee);

    /**
     * Déduit des jours du solde (avec gestion des urgences)
     * @return Détail de la déduction (jours déduits, jours non couverts)
     */
    SoldeDeductionResult decrementerSoldeConge(Long employeId, int joursDemandes, boolean isUrgent);

    /**
     * Ajoute des jours au solde (pour annulation)
     */
    void incrementerSoldeConge(Long employeId, int jours);
}