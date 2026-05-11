package com.codeWithProject.ecom.service.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopCandidatureResponse {

    private CandidatureResponseDTO candidature;

    private Integer scoreGlobal;

    private String niveauCompatibilite;
}
