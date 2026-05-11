package com.codeWithProject.ecom.service.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiMessageResponseDTO {

    private boolean success;

    private String message;
}