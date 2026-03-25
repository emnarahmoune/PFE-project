package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Employe;
import com.codeWithProject.ecom.entity.EmployeCompetence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeCompetenceRepository extends JpaRepository<EmployeCompetence, Long> {

    // ===== RECHERCHES PAR EMPLOYÉ =====
    List<EmployeCompetence> findByEmployeId(Long employeId);

    @Query("SELECT ec FROM EmployeCompetence ec WHERE ec.employe.id = :employeId ORDER BY ec.dateAcquisition DESC")
    List<EmployeCompetence> findCompetencesEmploye(@Param("employeId") Long employeId);

    // ===== RECHERCHES PAR COMPÉTENCE =====
    List<EmployeCompetence> findByCompetenceId(Long competenceId);

    @Query("SELECT ec FROM EmployeCompetence ec WHERE ec.competence.id = :competenceId ORDER BY ec.niveau DESC")
    List<EmployeCompetence> findEmployesParCompetence(@Param("competenceId") Long competenceId);

    // ===== RECHERCHES COMBINÉES =====
    @Query("SELECT ec FROM EmployeCompetence ec WHERE ec.employe.id = :employeId AND ec.competence.id = :competenceId")
    EmployeCompetence findByEmployeAndCompetence(@Param("employeId") Long employeId, @Param("competenceId") Long competenceId);

    boolean existsByEmployeIdAndCompetenceId(Long employeId, Long competenceId);

    // ===== RECHERCHES PAR NIVEAU =====
    List<EmployeCompetence> findByNiveau(String niveau);

    @Query("SELECT ec FROM EmployeCompetence ec WHERE ec.employe.id = :employeId AND ec.niveau = :niveau")
    List<EmployeCompetence> findCompetencesEmployeByNiveau(@Param("employeId") Long employeId, @Param("niveau") String niveau);

    @Query("SELECT ec FROM EmployeCompetence ec WHERE ec.niveau IN ('AVANCE', 'EXPERT')")
    List<EmployeCompetence> findCompetencesAvancees();

    // ===== RECHERCHES PAR CERTIFICATION =====
    List<EmployeCompetence> findByCertifieTrue();
    List<EmployeCompetence> findByCertifieFalse();

    @Query("SELECT ec FROM EmployeCompetence ec WHERE ec.certifie = true AND ec.dateExpirationCertification < CURRENT_DATE")
    List<EmployeCompetence> findCertificationsExpirees();

    @Query("SELECT ec FROM EmployeCompetence ec WHERE ec.certifie = true AND ec.dateExpirationCertification > CURRENT_DATE")
    List<EmployeCompetence> findCertificationsValides();

    @Query("SELECT ec FROM EmployeCompetence ec WHERE ec.certifie = true AND ec.dateExpirationCertification BETWEEN CURRENT_DATE AND :dateLimite")
    List<EmployeCompetence> findCertificationsAVenir(@Param("dateLimite") LocalDate dateLimite);

    // ===== RECHERCHES PAR VALIDATION =====
    List<EmployeCompetence> findByValideParManagerTrue();
    List<EmployeCompetence> findByValideParManagerFalse();

    @Query("SELECT ec FROM EmployeCompetence ec WHERE ec.valideParManager = false")
    List<EmployeCompetence> findCompetencesNonValidees();

    // ===== STATISTIQUES =====
    @Query("SELECT ec.niveau, COUNT(ec) FROM EmployeCompetence ec GROUP BY ec.niveau")
    List<Object[]> countByNiveau();

    @Query("SELECT ec.competence.categorie, COUNT(ec) FROM EmployeCompetence ec GROUP BY ec.competence.categorie")
    List<Object[]> countByCategorie();

    @Query("SELECT ec.employe.id, COUNT(ec) FROM EmployeCompetence ec GROUP BY ec.employe.id ORDER BY COUNT(ec) DESC")
    List<Object[]> findEmployesAvecPlusCompetences();

    @Query("SELECT ec.competence.id, ec.competence.nom, COUNT(ec) FROM EmployeCompetence ec GROUP BY ec.competence.id, ec.competence.nom ORDER BY COUNT(ec) DESC")
    List<Object[]> findCompetencesLesPlusAttribuees();

    @Query("SELECT AVG(CASE ec.niveau " +
            "WHEN 'DEBUTANT' THEN 1 " +
            "WHEN 'INTERMEDIAIRE' THEN 2 " +
            "WHEN 'AVANCE' THEN 3 " +
            "WHEN 'EXPERT' THEN 4 ELSE 0 END) FROM EmployeCompetence ec")
    Double niveauMoyenGlobal();

    // ===== RECHERCHES AVANCÉES =====
    @Query("SELECT ec FROM EmployeCompetence ec WHERE ec.employe.id = :employeId AND ec.niveau = 'EXPERT'")
    List<EmployeCompetence> findExpertisesEmploye(@Param("employeId") Long employeId);

    @Query("SELECT ec.employe FROM EmployeCompetence ec WHERE ec.competence.id = :competenceId AND ec.niveau IN ('AVANCE', 'EXPERT')")
    List<Employe> findExpertsParCompetence(@Param("competenceId") Long competenceId);
    @Query("SELECT ec FROM EmployeCompetence ec WHERE ec.dateAcquisition >= :date")
    List<EmployeCompetence> findCompetencesAcquisesRecement(@Param("date") LocalDate date);
}