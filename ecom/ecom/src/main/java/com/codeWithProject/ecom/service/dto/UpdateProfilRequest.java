package com.codeWithProject.ecom.service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfilRequest {
    private String telephone;
    private String adresse;
    private String photo;
}