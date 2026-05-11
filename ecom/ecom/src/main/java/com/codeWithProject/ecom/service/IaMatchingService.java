package com.codeWithProject.ecom.service;


import com.codeWithProject.ecom.service.dto.CvAnalyseResponseDTO;
import com.codeWithProject.ecom.service.dto.IaMatchingResponseDTO;
import com.codeWithProject.ecom.entity.Candidature;

public interface IaMatchingService {

    IaMatchingResponseDTO analyserCandidature(Candidature candidature);

    CvAnalyseResponseDTO getAnalyseByCandidature(Long candidatureId);
}