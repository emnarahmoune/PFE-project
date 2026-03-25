package com.codeWithProject.ecom.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Réponse API standardisée
 * Utilisée pour toutes les réponses des contrôleurs
 * @param <T> Le type de données retourné
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String timestamp;
    private int statusCode;

    /**
     * Crée une réponse de succès avec données
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(java.time.LocalDateTime.now().toString())
                .statusCode(HttpStatus.OK.value())
                .build();
    }

    /**
     * Crée une réponse de succès sans données
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(java.time.LocalDateTime.now().toString())
                .statusCode(HttpStatus.OK.value())
                .build();
    }

    /**
     * Crée une réponse de création (201)
     */
    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(java.time.LocalDateTime.now().toString())
                .statusCode(HttpStatus.CREATED.value())
                .build();
    }

    /**
     * Crée une réponse d'erreur
     */
    public static <T> ApiResponse<T> error(String message, HttpStatus status) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(java.time.LocalDateTime.now().toString())
                .statusCode(status.value())
                .build();
    }
}