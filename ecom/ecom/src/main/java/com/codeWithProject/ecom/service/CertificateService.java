package com.codeWithProject.ecom.service;

import com.codeWithProject.ecom.entity.Certificate;
import java.util.List;

public interface CertificateService {

    Certificate generateCertificate(Long employeId, Long formationId);

    List<Certificate> getMyCertificates(Long employeId);
}