package com.codeWithProject.ecom.service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedVideoDto {

    private String titre;

    private String urlYoutube;

    private Integer ordre;
}