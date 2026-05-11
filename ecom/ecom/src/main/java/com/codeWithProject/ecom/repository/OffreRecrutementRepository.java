package com.codeWithProject.ecom.repository;


import com.codeWithProject.ecom.entity.OffreRecrutement;
import com.codeWithProject.ecom.entity.enums.StatutOffreRecrutement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OffreRecrutementRepository extends JpaRepository<OffreRecrutement, Long> {

    List<OffreRecrutement> findByStatutOrderByDatePublicationDesc(StatutOffreRecrutement statut);

    List<OffreRecrutement> findAllByOrderByIdDesc();

    List<OffreRecrutement> findByTitrePosteContainingIgnoreCaseOrderByIdDesc(String titrePoste);

    List<OffreRecrutement> findByDepartementContainingIgnoreCaseOrderByIdDesc(String departement);
}