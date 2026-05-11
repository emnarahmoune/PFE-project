package com.codeWithProject.ecom.service.dto;


import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvAnalyseResponseDTO {

    private Long id;

    private Long candidatureId;

    private Long offreId;

    private Long employeId;

    private String cvFileName;

    private String cvContentType;

    private String texteExtrait;

    private List<String> competencesDetectees;

    private List<String> technologiesDetectees;

    private List<String> experiencesDetectees;

    private Integer anneesExperienceEstimees;

    private String resumeProfil;

    private List<String> pointsForts;

    private List<String> pointsFaibles;

    private LocalDateTime dateAnalyse;
}