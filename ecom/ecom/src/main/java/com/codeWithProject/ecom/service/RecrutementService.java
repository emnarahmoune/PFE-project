package com.codeWithProject.ecom.service;


import com.codeWithProject.ecom.service.dto.ChangementStatutOffreRequestDTO;
import com.codeWithProject.ecom.service.dto.OffreRecrutementRequest;
import com.codeWithProject.ecom.service.dto.OffreRecrutementResponse;

import java.util.List;

public interface RecrutementService {

    List<OffreRecrutementResponse> getAllOffres();

    List<OffreRecrutementResponse> getOffresOuvertes();

    OffreRecrutementResponse getOffreById(Long id);

    OffreRecrutementResponse createOffre(OffreRecrutementRequest request);

    OffreRecrutementResponse updateOffre(Long id, OffreRecrutementRequest request);

    OffreRecrutementResponse changerStatutOffre(Long id, ChangementStatutOffreRequestDTO request);

    void deleteOffre(Long id);
}