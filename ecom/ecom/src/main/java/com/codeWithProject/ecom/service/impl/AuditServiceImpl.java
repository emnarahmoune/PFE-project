package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.AuditLog;
import com.codeWithProject.ecom.repository.AuditLogRepository;
import com.codeWithProject.ecom.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void logAction(
            String action,
            Long actorId,
            String actorEmail,
            String targetType,
            Long targetId,
            String targetEmail,
            String details
    ) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .targetType(targetType)
                .targetId(targetId)
                .targetEmail(targetEmail)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }

    @Override
    public List<AuditLog> getAll() {
        return auditLogRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public List<AuditLog> getByTarget(String targetType, Long targetId) {
        return auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId);
    }

    @Override
    public List<AuditLog> getByActor(Long actorId) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId);
    }

    @Override
    public List<AuditLog> getByAction(String action) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action);
    }
}
