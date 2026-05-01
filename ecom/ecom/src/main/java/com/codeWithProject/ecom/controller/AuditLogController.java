package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.entity.AuditLog;
import com.codeWithProject.ecom.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        return ResponseEntity.ok(auditService.getAll());
    }

    @GetMapping("/target/{targetType}/{targetId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByTarget(
            @PathVariable String targetType,
            @PathVariable Long targetId
    ) {
        return ResponseEntity.ok(auditService.getByTarget(targetType, targetId));
    }

    @GetMapping("/actor/{actorId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByActor(
            @PathVariable Long actorId
    ) {
        return ResponseEntity.ok(auditService.getByActor(actorId));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByAction(
            @PathVariable String action
    ) {
        return ResponseEntity.ok(auditService.getByAction(action));
    }
}