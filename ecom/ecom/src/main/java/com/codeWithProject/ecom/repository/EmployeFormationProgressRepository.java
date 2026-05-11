package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.EmployeFormationProgress;
import com.codeWithProject.ecom.entity.FormationVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeFormationProgressRepository
        extends JpaRepository<EmployeFormationProgress, Long> {

    long countByEmployeIdAndVideo_Formation_IdAndCompletedTrue(
            Long empId,
            Long formationId
    );

    List<EmployeFormationProgress> findByEmployeIdAndVideo_Formation_IdAndCompletedTrue(
            Long empId,
            Long formationId
    );

    boolean existsByEmployeIdAndVideo(
            Long employeId,
            FormationVideo video
    );

    Optional<EmployeFormationProgress> findByEmployeIdAndVideo(
            Long employeId,
            FormationVideo video
    );
}