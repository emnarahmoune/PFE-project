package com.codeWithProject.ecom.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignManagerDTO {
    private Long employeId;
    private Long managerId;
    private String employeEmail;
    private String managerEmail;
}