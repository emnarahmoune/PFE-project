package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByEmploye_IdAndFormation_Id(Long employeId, Long formationId);

List<Certificate> findByEmploye_Id(Long employeId);
}