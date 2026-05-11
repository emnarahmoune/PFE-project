package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.service.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import com.codeWithProject.ecom.service.dto.DashboardManagerBiDTO;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class BiDashboardRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public DashboardAdminBiDTO getDashboardAdminBi() {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
            SELECT
                effectif_total,
                employes_actifs,
                employes_inactifs,
                total_managers,
                total_departements,
                masse_salariale,
                salaire_moyen,
                total_demandes_conge,
                conges_en_attente,
                conges_approuves,
                jours_absence,
                total_evaluations,
                note_moyenne_globale,
                total_formations,
                total_competences,
                score_risque_moyen,
                employes_risque_eleve
            FROM vue_bi_dashboard_admin
            LIMIT 1
        """).getSingleResult();

        DashboardAdminBiDTO dto = new DashboardAdminBiDTO();

        dto.setEffectifTotal(toLong(row[0]));
        dto.setEmployesActifs(toLong(row[1]));
        dto.setEmployesInactifs(toLong(row[2]));
        dto.setTotalManagers(toLong(row[3]));
        dto.setTotalDepartements(toLong(row[4]));

        dto.setMasseSalariale(toBigDecimal(row[5]));
        dto.setSalaireMoyen(toBigDecimal(row[6]));

        dto.setTotalDemandesConge(toLong(row[7]));
        dto.setCongesEnAttente(toLong(row[8]));
        dto.setCongesApprouves(toLong(row[9]));
        dto.setJoursAbsence(toLong(row[10]));

        dto.setTotalEvaluations(toLong(row[11]));
        dto.setNoteMoyenneGlobale(toBigDecimal(row[12]));

        dto.setTotalFormations(toLong(row[13]));
        dto.setTotalCompetences(toLong(row[14]));

        dto.setScoreRisqueMoyen(toBigDecimal(row[15]));
        dto.setEmployesRisqueEleve(toLong(row[16]));

        dto.setParDepartement(getParDepartement());
        dto.setRepartitionStatut(getRepartitionStatut());
        dto.setTopCompetences(getTopCompetences());
        dto.setRecentEmployees(getRecentEmployees());

        return dto;
    }

    private Map<String, Long> getParDepartement() {
        List<Object[]> rows = entityManager.createNativeQuery("""
            SELECT departement, COUNT(*) AS total
            FROM vue_bi_employes
            WHERE departement IS NOT NULL
            GROUP BY departement
            ORDER BY total DESC
        """).getResultList();

        Map<String, Long> result = new LinkedHashMap<>();

        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), toLong(row[1]));
        }

        return result;
    }

    private Map<String, Long> getRepartitionStatut() {
        List<Object[]> rows = entityManager.createNativeQuery("""
            SELECT statut_activite, COUNT(*) AS total
            FROM vue_bi_employes
            WHERE statut_activite IS NOT NULL
            GROUP BY statut_activite
            ORDER BY total DESC
        """).getResultList();

        Map<String, Long> result = new LinkedHashMap<>();

        for (Object[] row : rows) {
            result.put(String.valueOf(row[0]), toLong(row[1]));
        }

        return result;
    }

    private List<TopCompetenceDTO> getTopCompetences() {
        List<Object[]> rows = entityManager.createNativeQuery("""
            SELECT competence_nom, COUNT(*) AS total
            FROM vue_bi_competences
            WHERE competence_nom IS NOT NULL
            GROUP BY competence_nom
            ORDER BY total DESC
            LIMIT 5
        """).getResultList();

        Long max = rows.stream()
                .map(row -> toLong(row[1]))
                .max(Long::compareTo)
                .orElse(1L);

        return rows.stream()
                .map(row -> {
                    Long count = toLong(row[1]);
                    double percentage = max == 0 ? 0 : (count * 100.0 / max);

                    return new TopCompetenceDTO(
                            String.valueOf(row[0]),
                            count,
                            percentage
                    );
                })
                .toList();
    }

    private List<RecentEmployeeDTO> getRecentEmployees() {
        List<Object[]> rows = entityManager.createNativeQuery("""
            SELECT employe_id, nom, prenom, poste, departement, date_embauche
            FROM vue_bi_employes
            WHERE role = 'EMPLOYE'
            ORDER BY date_embauche DESC
            LIMIT 5
        """).getResultList();

        return rows.stream()
                .map(row -> new RecentEmployeeDTO(
                        toLong(row[0]),
                        toStringValue(row[1]),
                        toStringValue(row[2]),
                        toStringValue(row[3]),
                        toStringValue(row[4]),
                        toLocalDate(row[5])
                ))
                .toList();
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        return ((Number) value).longValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private java.time.LocalDate toLocalDate(Object value) {
        if (value == null) return null;

        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        if (value instanceof java.time.LocalDate localDate) {
            return localDate;
        }

        return java.time.LocalDate.parse(value.toString());
    }



    public DashboardManagerBiDTO getManagerDashboardBi(Long managerId) {
    Object[] row = (Object[]) entityManager.createNativeQuery("""
        SELECT
            COUNT(*) AS total_employes,
            SUM(CASE WHEN actif = 1 THEN 1 ELSE 0 END) AS employes_actifs,
            IFNULL(AVG(score_risque), 0) AS score_risque_moyen,
            SUM(CASE WHEN niveau_risque = 'Élevé' THEN 1 ELSE 0 END) AS employes_risque_eleve
        FROM vue_bi_risque_turnover
        WHERE manager_id = :managerId
    """)
    .setParameter("managerId", managerId)
    .getSingleResult();

    Long totalEmployes = toLong(row[0]);
    Long employesActifs = toLong(row[1]);
    BigDecimal scoreRisqueMoyen = toBigDecimal(row[2]);
    Long employesRisqueEleve = toLong(row[3]);

    Long congesEnAttente = getManagerCongesEnAttente(managerId);
    Long joursAbsence = getManagerJoursAbsence(managerId);

    BigDecimal tauxPresence = BigDecimal.ZERO;
    if (employesActifs > 0) {
        double taux = ((employesActifs * 22.0 - joursAbsence) / (employesActifs * 22.0)) * 100.0;
        tauxPresence = BigDecimal.valueOf(Math.max(0, taux));
    }

    DashboardManagerBiDTO dto = new DashboardManagerBiDTO();
    dto.setTotalEmployes(totalEmployes);
    dto.setEmployesActifs(employesActifs);
    dto.setCongesEnAttente(congesEnAttente);
    dto.setJoursAbsence(joursAbsence);
    dto.setTauxPresence(tauxPresence);
    dto.setScoreRisqueMoyen(scoreRisqueMoyen);
    dto.setEmployesRisqueEleve(employesRisqueEleve);
    dto.setTopCompetences(getManagerTopCompetences(managerId));

    return dto;
}

private Long getManagerCongesEnAttente(Long managerId) {
    Object result = entityManager.createNativeQuery("""
        SELECT COUNT(*)
        FROM vue_bi_conges
        WHERE manager_id = :managerId
          AND statut_conge = 'EN_ATTENTE'
    """)
    .setParameter("managerId", managerId)
    .getSingleResult();

    return toLong(result);
}

private Long getManagerJoursAbsence(Long managerId) {
    Object result = entityManager.createNativeQuery("""
        SELECT IFNULL(SUM(jours_absence_approuves), 0)
        FROM vue_bi_conges
        WHERE manager_id = :managerId
    """)
    .setParameter("managerId", managerId)
    .getSingleResult();

    return toLong(result);
}

private List<TopCompetenceDTO> getManagerTopCompetences(Long managerId) {
    List<Object[]> rows = entityManager.createNativeQuery("""
        SELECT competence_nom, COUNT(*) AS total
        FROM vue_bi_competences
        WHERE manager_id = :managerId
          AND competence_nom IS NOT NULL
        GROUP BY competence_nom
        ORDER BY total DESC
        LIMIT 5
    """)
    .setParameter("managerId", managerId)
    .getResultList();

    Long max = rows.stream()
            .map(row -> toLong(row[1]))
            .max(Long::compareTo)
            .orElse(1L);

    return rows.stream()
            .map(row -> {
                Long count = toLong(row[1]);
                double percentage = max == 0 ? 0 : (count * 100.0 / max);

                return new TopCompetenceDTO(
                        String.valueOf(row[0]),
                        count,
                        percentage
                );
            })
            .toList();
}
}