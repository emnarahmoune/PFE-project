package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.DemandeConge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DemandeCongeRepository extends JpaRepository<DemandeConge, Long> {

    // ===== RECHERCHES PAR EMPLOYÉ =====
    List<DemandeConge> findByEmployeId(Long employeId);

    // Nécessaire pour hasConflitDates
    List<DemandeConge> findByEmployeIdAndStatutIn(Long employeId, List<String> statuts);

    @Query("SELECT d FROM DemandeConge d WHERE d.employe.id = :employeId ORDER BY d.dateDemande DESC")
    List<DemandeConge> findHistoriqueEmploye(@Param("employeId") Long employeId);

    // ===== RECHERCHES PAR MANAGER =====
    List<DemandeConge> findByManagerId(Long managerId);

    @Query("SELECT d FROM DemandeConge d WHERE d.manager.id = :managerId AND d.statut = 'EN_ATTENTE' ORDER BY d.dateDemande ASC")
    List<DemandeConge> findDemandesEnAttentePourManager(@Param("managerId") Long managerId);

    // ===== RECHERCHES PAR STATUT =====
    List<DemandeConge> findByStatut(String statut);

    @Query("SELECT d FROM DemandeConge d WHERE d.statut = 'EN_ATTENTE' ORDER BY d.dateDemande ASC")
    List<DemandeConge> findDemandesEnAttente();

    @Query("SELECT d FROM DemandeConge d WHERE d.statut = 'EN_ATTENTE' AND d.urgente = true")
    List<DemandeConge> findDemandesUrgentes();

    @Query("SELECT d FROM DemandeConge d WHERE d.statut = 'EN_ATTENTE' AND d.dateDebut <= :dateLimite")
    List<DemandeConge> findDemandesUrgentesAvantDate(@Param("dateLimite") LocalDate dateLimite);

    // ===== RECHERCHES PAR TYPE =====
    List<DemandeConge> findByType(String type);

    // ===== RECHERCHES PAR DATES =====
    List<DemandeConge> findByDateDebutBetween(LocalDate debut, LocalDate fin);
    List<DemandeConge> findByDateFinBetween(LocalDate debut, LocalDate fin);
    List<DemandeConge> findByDateDemandeBetween(LocalDate debut, LocalDate fin);

    @Query("SELECT d FROM DemandeConge d WHERE d.dateDebut <= CURRENT_DATE AND d.dateFin >= CURRENT_DATE AND d.statut = 'APPROUVE'")
    List<DemandeConge> findCongesEnCours();

    @Query("SELECT d FROM DemandeConge d WHERE d.dateDebut <= :date AND d.dateFin >= :date AND d.statut = 'APPROUVE'")
    List<DemandeConge> findCongesPourDate(@Param("date") LocalDate date);

    long countByStatut(String statut);

    // ===== STATISTIQUES =====
    @Query("SELECT COUNT(d) FROM DemandeConge d WHERE d.employe.id = :employeId AND d.statut = 'APPROUVE' AND YEAR(d.dateDebut) = :annee")
    long countCongesPrisAnnee(@Param("employeId") Long employeId, @Param("annee") int annee);

    @Query("SELECT d.employe.id, COUNT(d) FROM DemandeConge d GROUP BY d.employe.id ORDER BY COUNT(d) DESC")
    List<Object[]> countDemandesByEmploye();

    @Query("SELECT d.type, COUNT(d) FROM DemandeConge d GROUP BY d.type")
    List<Object[]> countByType();

    @Query("SELECT d.statut, COUNT(d) FROM DemandeConge d GROUP BY d.statut")
    List<Object[]> countByStatut();

    @Query("SELECT d.employe.departement, COUNT(d) FROM DemandeConge d GROUP BY d.employe.departement")
    List<Object[]> countByDepartement();

    @Query("SELECT MONTH(d.dateDebut), COUNT(d) FROM DemandeConge d WHERE YEAR(d.dateDebut) = :annee GROUP BY MONTH(d.dateDebut) ORDER BY MONTH(d.dateDebut)")
    List<Object[]> countByMois(@Param("annee") int annee);

    @Query("SELECT COUNT(d) FROM DemandeConge d WHERE d.employe.id = :employeId AND d.statut = :statut")
    long countByEmployeIdAndStatut(@Param("employeId") Long employeId, @Param("statut") String statut);

    // ===== MÉTHODES POUR WORKFLOW CAMUNDA =====

    // Pour récupérer une demande par processInstanceId
    @Query("SELECT d FROM DemandeConge d WHERE d.processInstanceId = :processInstanceId")
    Optional<DemandeConge> findByProcessInstanceId(@Param("processInstanceId") String processInstanceId);

    // Pour les demandes orphelines (sans processInstanceId)
    @Query("SELECT d FROM DemandeConge d WHERE d.processInstanceId IS NULL AND d.statut = 'EN_ATTENTE'")
    List<DemandeConge> findByProcessInstanceIdIsNull();

    // Pour les demandes en attente avec plus de 10 jours (pour le RH)
    @Query("SELECT d FROM DemandeConge d WHERE d.statut = 'EN_ATTENTE' AND d.joursOuvres > 10 ORDER BY d.dateDemande ASC")
    List<DemandeConge> findDemandesEnAttentePlusDe10Jours();

    // Pour les demandes en attente avec moins ou égal à 10 jours (pour le Manager)
    @Query("SELECT d FROM DemandeConge d WHERE d.statut = 'EN_ATTENTE' AND d.joursOuvres <= 10 ORDER BY d.dateDemande ASC")
    List<DemandeConge> findDemandesEnAttenteMoinsDe10Jours();

    // Pour trouver l'ID de la tâche Camunda
    @Query("SELECT d.currentTaskId FROM DemandeConge d WHERE d.processInstanceId = :processInstanceId")
    Optional<String> findCurrentTaskIdByProcessInstanceId(@Param("processInstanceId") String processInstanceId);
}