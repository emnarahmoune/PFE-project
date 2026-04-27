package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.Parametre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametreRepository extends JpaRepository<Parametre, String> {
}