package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.DemandeConge;
import com.codeWithProject.ecom.service.dto.CalendarEventDTO;
import com.codeWithProject.ecom.service.dto.DemandeRefusDetailsDTO;
import com.codeWithProject.ecom.service.dto.DemandeRefusManagerDTO;
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
    @Query("SELECT COALESCE(SUM(d.joursOuvres), 0) FROM DemandeConge d WHERE d.employe.id = :employeId AND d.statut = 'APPROUVE' AND YEAR(d.dateDebut) = :annee")
    int sumJoursOuvresApprouvesAnnee(@Param("employeId") Long employeId, @Param("annee") int annee);
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
    @Query("SELECT d FROM DemandeConge d WHERE d.processInstanceId = :processInstanceId")
    Optional<DemandeConge> findByProcessInstanceId(@Param("processInstanceId") String processInstanceId);
    @Query("SELECT d FROM DemandeConge d WHERE d.processInstanceId IS NULL AND d.statut = 'EN_ATTENTE'")
    List<DemandeConge> findByProcessInstanceIdIsNull();
    @Query("SELECT d FROM DemandeConge d WHERE d.statut = 'EN_ATTENTE' AND d.joursOuvres > 10 ORDER BY d.dateDemande ASC")
    List<DemandeConge> findDemandesEnAttentePlusDe10Jours();
    @Query("SELECT d FROM DemandeConge d WHERE d.statut = 'EN_ATTENTE' AND d.joursOuvres <= 10 ORDER BY d.dateDemande ASC")
    List<DemandeConge> findDemandesEnAttenteMoinsDe10Jours();
    @Query("SELECT d.currentTaskId FROM DemandeConge d WHERE d.processInstanceId = :processInstanceId")
    Optional<String> findCurrentTaskIdByProcessInstanceId(@Param("processInstanceId") String processInstanceId);
    List<DemandeConge> findByStatutAndJoursOuvresGreaterThan(String statut, int jours);


    // Supprimer la première version erronée de findAllForCalendar().
// Garder celle-ci :
    @Query("SELECT new com.codeWithProject.ecom.service.dto.CalendarEventDTO(" +
            "d.id, " +
            "CONCAT(e.prenom, ' ', e.nom, ' (', d.joursOuvres, 'j)'), " +
            "d.dateDebut, d.dateFin, " +
            "CASE d.statut " +
            "  WHEN 'APPROUVE' THEN '#10b981' " +
            "  WHEN 'EN_ATTENTE_RH' THEN '#f59e0b' " +
            "  WHEN 'REFUSE_MANAGER' THEN '#ef4444' " +
            "  WHEN 'REFUSE' THEN '#ef4444' " +
            "  ELSE '#94a3b8' END, " +
            "d.statut, d.type, e.nom, e.prenom) " +
            "FROM DemandeConge d JOIN d.employe e " +
            "WHERE d.statut IN ('APPROUVE', 'EN_ATTENTE_RH', 'REFUSE_MANAGER', 'REFUSE')")
    List<CalendarEventDTO> findAllForCalendar();

    @Query("SELECT new com.codeWithProject.ecom.service.dto.DemandeRefusManagerDTO(" +
            "d.id, e.prenom, e.nom, e.departement, CONCAT(m.prenom, ' ', m.nom), " +
            "d.dateDebut, d.dateFin, d.motifRefus) " +
            "FROM DemandeConge d " +
            "JOIN d.employe e " +
            "LEFT JOIN d.manager m " +
            "WHERE d.statut = 'REFUSE_MANAGER' " +
            "ORDER BY d.dateDecision DESC")
    List<DemandeRefusManagerDTO> findDemandesRefuseesParManager();
    // Dans DemandeCongeRepository.java
    @Query("SELECT COALESCE(SUM(d.joursOuvres), 0) FROM DemandeConge d " +
            "WHERE d.employe.id = :employeId AND d.statut = 'APPROUVE' " +
            "AND d.type IN ('MALADIE', 'SANS_SOLDE') AND d.dateDebut BETWEEN :debut AND :fin")
    int sumJoursAbsenceEntreDates(@Param("employeId") Long employeId,
                                  @Param("debut") LocalDate debut,
                                  @Param("fin") LocalDate fin);


    // Dans DemandeCongeRepository.java
    @Query("SELECT COALESCE(SUM(d.joursOuvres), 0) FROM DemandeConge d " +
            "WHERE d.employe.id = :employeId AND d.statut = 'APPROUVE' " +
            "AND d.type IN ('MALADIE', 'SANS_SOLDE') AND YEAR(d.dateDebut) = :annee")
    int sumJoursAbsence(@Param("employeId") Long employeId, @Param("annee") int annee);




    @Query("SELECT new com.codeWithProject.ecom.service.dto.DemandeRefusDetailsDTO(" +
            "d.id, e.prenom, e.nom, e.email, e.departement, CONCAT(m.prenom, ' ', m.nom), " +
            "d.dateDebut, d.dateFin, d.joursOuvres, d.type, d.motifRefus, " +
            "d.dateSoumission, d.dateRefusManager, d.commentaire, d.piecesJointes) " +
            "FROM DemandeConge d " +
            "JOIN d.employe e " +
            "LEFT JOIN d.manager m " +
            "WHERE d.id = :id AND d.statut IN ('REFUSE_MANAGER', 'REFUSE')")
    Optional<DemandeRefusDetailsDTO> findRefusDetailsById(@Param("id") Long id);

    // Alternative générique
    List<DemandeConge> findByStatutAndManagerIsNotNull(String statut);


    @Query("SELECT new com.codeWithProject.ecom.service.dto.CalendarEventDTO(" +
            "d.id, " +
            "CONCAT(e.prenom, ' ', e.nom, ' (', d.joursOuvres, 'j)'), " +
            "d.dateDebut, d.dateFin, " +
            "CASE d.statut " +
            "  WHEN 'APPROUVE' THEN '#10b981' " +
            "  WHEN 'EN_ATTENTE_RH' THEN '#f59e0b' " +
            "  WHEN 'REFUSE_MANAGER' THEN '#ef4444' " +
            "  WHEN 'REFUSE' THEN '#ef4444' " +
            "  ELSE '#94a3b8' END, " +
            "d.statut, d.type, e.nom, e.prenom) " +
            "FROM DemandeConge d JOIN d.employe e " +
            "WHERE e.id IN :employesIds " +
            "AND d.statut IN ('APPROUVE', 'EN_ATTENTE_RH', 'REFUSE_MANAGER', 'REFUSE')")
    List<CalendarEventDTO> findCalendarEventsForEmployes(@Param("employesIds") List<Long> employesIds);

}