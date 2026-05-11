package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.service.dto.CandidatureResponseDTO;
import com.codeWithProject.ecom.service.dto.DecisionCandidatureRequestDTO;
import com.codeWithProject.ecom.service.dto.TopCandidatureResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CandidatureService {

    CandidatureResponseDTO postuler(Long offreId, Long employeId, String motivation, MultipartFile cv);

    List<CandidatureResponseDTO> getMesCandidatures(Long employeId);

    boolean hasAlreadyApplied(Long offreId, Long employeId);

    List<CandidatureResponseDTO> getCandidaturesByOffre(Long offreId);

    List<TopCandidatureResponse> getTopCandidatures(Long offreId, int limit);

    CandidatureResponseDTO accepterCandidature(Long candidatureId, DecisionCandidatureRequestDTO request);

    CandidatureResponseDTO refuserCandidature(Long candidatureId, DecisionCandidatureRequestDTO request);

    CandidatureResponseDTO relancerAnalyseIa(Long candidatureId);
}