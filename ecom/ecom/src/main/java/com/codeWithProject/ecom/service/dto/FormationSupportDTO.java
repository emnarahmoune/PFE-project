package com.codeWithProject.ecom.service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationSupportDTO {

    private Long id;
    private String titre;
    private String fichierUrl;
    private Integer ordre;
}