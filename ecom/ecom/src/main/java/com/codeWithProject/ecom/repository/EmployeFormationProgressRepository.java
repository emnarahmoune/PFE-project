package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.EmployeFormation;
import com.codeWithProject.ecom.entity.EmployeFormationProgress;
import com.codeWithProject.ecom.entity.FormationVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeFormationProgressRepository
        extends JpaRepository<EmployeFormationProgress, Long> {

    long countByEmployeIdAndVideo_Formation_IdAndCompletedTrue(Long empId, Long formationId);
List<EmployeFormationProgress> 
findByEmployeIdAndVideo_Formation_IdAndCompletedTrue(Long empId, Long formationId);
List<EmployeFormationProgress> findByFormationId(Long formationId);

boolean existsByEmployeIdAndVideo(Long employeId, FormationVideo video);


}