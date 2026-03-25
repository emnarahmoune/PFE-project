package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.CompetenceDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface de service pour la gestion des compétences
 */
public interface CompetenceService {

    /**
     * Récupère toutes les compétences
     */
    List<CompetenceDTO> findAll();

    /**
     * Récupère toutes les compétences avec pagination
     */
    Page<CompetenceDTO> findAll(Pageable pageable);

    /**
     * Récupère une compétence par son ID
     */
    Optional<CompetenceDTO> findById(Long id);

    /**
     * Récupère une compétence par son nom
     */
    Optional<CompetenceDTO> findByNom(String nom);

    /**
     * Récupère les compétences par catégorie
     */
    List<CompetenceDTO> findByCategorie(String categorie);

    /**
     * Récupère toutes les catégories distinctes
     */
    List<String> findAllCategories();

    /**
     * Crée une nouvelle compétence
     */
    CompetenceDTO create(CompetenceDTO dto);

    /**
     * Met à jour une compétence existante
     */
    CompetenceDTO update(Long id, CompetenceDTO dto);

    /**
     * Supprime une compétence
     */
    void delete(Long id);

    /**
     * Vérifie si un nom de compétence existe déjà
     */
    boolean existsByNom(String nom);

    /**
     * Compte le nombre total de compétences
     */
    long count();

    /**
     * Récupère les statistiques par catégorie
     */
    Map<String, Long> getStatsByCategorie();

    /**
     * Recherche des compétences par mot-clé
     */
    List<CompetenceDTO> search(String keyword);

    /**
     * Récupère les compétences les plus utilisées
     */
    List<CompetenceDTO> findTopCompetences(int limit);

    /**
     * Récupère les compétences non attribuées
     */
    List<CompetenceDTO> findCompetencesNonAttribuees();
}