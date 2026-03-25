package com.codeWithProject.ecom.controller;

import com.codeWithProject.ecom.controller.dto.ApiResponse;
import com.codeWithProject.ecom.service.exception.BusinessException;
import com.codeWithProject.ecom.service.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 404 ────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("404 [{}] : {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND));
    }

    // ── 400 BusinessException ──────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(
            BusinessException ex, HttpServletRequest req) {
        log.warn("400 [{}] code={} : {}", req.getRequestURI(), ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    // ── 400 Validation @Valid ──────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err ->
                errors.put(((FieldError) err).getField(), err.getDefaultMessage()));
        log.warn("400 Validation [{}] : {}", req.getRequestURI(), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false).message("Erreur de validation").data(errors)
                        .timestamp(LocalDateTime.now().toString())
                        .statusCode(HttpStatus.BAD_REQUEST.value()).build());
    }

    // ── 400 Contrainte SQL (NOT NULL, UNIQUE…) ─────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest req) {
        String msg = extraireMessageSQL(ex.getMostSpecificCause());
        log.warn("400 DataIntegrity [{}] : {}", req.getRequestURI(), msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg, HttpStatus.BAD_REQUEST));
    }

    // ── 500 TransactionSystemException ────────────────────────
    // Intercepte les erreurs au commit JPA (@PreUpdate, @PrePersist, contraintes)
    // et expose la vraie cause racine au lieu d'un message générique
    @ExceptionHandler({TransactionSystemException.class, JpaSystemException.class})
    public ResponseEntity<ApiResponse<Void>> handleTransactionSystem(
            Exception ex, HttpServletRequest req) {

        // Dérouler la chaîne de causes pour trouver la vraie exception
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String rootMsg = cause.getClass().getSimpleName() + " : " + cause.getMessage();
        log.error("500 TransactionSystem [{}] cause racine : {}", req.getRequestURI(), rootMsg, ex);

        // Si c'est une erreur de validation @PreUpdate/@PrePersist → 400
        if (cause instanceof IllegalStateException
                || cause instanceof IllegalArgumentException
                || cause instanceof StringIndexOutOfBoundsException) {
            String msg = cause instanceof StringIndexOutOfBoundsException
                    ? "Données invalides en base (champ vide ou null) : " + cause.getMessage()
                    : cause.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(msg, HttpStatus.BAD_REQUEST));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(rootMsg, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    // ── 400 Mauvais type paramètre URL ─────────────────────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String msg = String.format("Paramètre '%s' invalide : '%s' attendu en %s",
                ex.getName(), ex.getValue(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "?");
        log.warn("400 TypeMismatch [{}] : {}", req.getRequestURI(), msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg, HttpStatus.BAD_REQUEST));
    }

    // ── 500 catch-all ──────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("500 [{}] {} : {}", req.getRequestURI(),
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
        String msg = ex.getClass().getSimpleName() + " : "
                + (ex.getMessage() != null ? ex.getMessage() : "erreur interne");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(msg, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    // ── Utilitaire ─────────────────────────────────────────────
    private String extraireMessageSQL(Throwable cause) {
        if (cause == null) return "Violation de contrainte base de données";
        String raw = cause.getMessage();
        if (raw == null) return "Violation de contrainte base de données";
        if (raw.contains("cannot be null")) {
            String col = raw.replaceAll(".*Column '([^']+)' cannot be null.*", "$1");
            return "Le champ '" + col + "' est obligatoire";
        }
        if (raw.contains("Duplicate entry")) {
            String val = raw.replaceAll(".*Duplicate entry '([^']+)'.*", "$1");
            return "La valeur '" + val + "' existe déjà";
        }
        return "Violation de contrainte : " + raw;
    }
}