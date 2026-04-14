package com.codeWithProject.ecom.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String error;
    private LocalDateTime timestamp;
    private int statusCode;

    // ===== SUCCESS =====
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        response.setStatusCode(HttpStatus.OK.value());
        return response;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = success(data);
        response.setMessage(message);
        return response;
    }

    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        response.setStatusCode(HttpStatus.OK.value());
        return response;
    }

    // ===== CREATED =====
    public static <T> ApiResponse<T> created(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        response.setStatusCode(HttpStatus.CREATED.value());
        return response;
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        ApiResponse<T> response = created(data);
        response.setMessage(message);
        return response;
    }

    public static <T> ApiResponse<T> created(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        response.setStatusCode(HttpStatus.CREATED.value());
        return response;
    }

    // ===== ERROR =====
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setError(message);
        response.setTimestamp(LocalDateTime.now());
        response.setStatusCode(HttpStatus.BAD_REQUEST.value());
        return response;
    }

    public static <T> ApiResponse<T> error(String message, HttpStatus status) {
        ApiResponse<T> response = error(message);
        response.setStatusCode(status.value());
        return response;
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return error(message, status);
    }
}