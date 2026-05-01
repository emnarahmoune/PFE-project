package com.codeWithProject.ecom.repository;

import com.codeWithProject.ecom.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);

    List<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId);

    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
}