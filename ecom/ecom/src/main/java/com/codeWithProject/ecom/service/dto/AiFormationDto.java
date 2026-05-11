package com.codeWithProject.ecom.service.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiFormationDto {

    private Long id;

    private String titre;

    private String description;

    private String domaine;

    private String niveau;

    private Integer dureeHeures;

    private List<String> competences;
}