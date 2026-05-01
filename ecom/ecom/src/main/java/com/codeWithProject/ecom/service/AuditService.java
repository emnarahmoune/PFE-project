package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.AuditLog;

import java.util.List;

public interface AuditService {

    void logAction(
            String action,
            Long actorId,
            String actorEmail,
            String targetType,
            Long targetId,
            String targetEmail,
            String details
    );

    List<AuditLog> getAll();

    List<AuditLog> getByTarget(String targetType, Long targetId);

    List<AuditLog> getByActor(Long actorId);

    List<AuditLog> getByAction(String action);
}
