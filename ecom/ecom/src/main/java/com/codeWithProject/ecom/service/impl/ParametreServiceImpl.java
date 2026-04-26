package com.codeWithProject.ecom.service.impl;

import com.codeWithProject.ecom.entity.Parametre;
import com.codeWithProject.ecom.repository.ParametreRepository;
import com.codeWithProject.ecom.service.ParametreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Service("parametreService")
@RequiredArgsConstructor
@Slf4j
public class ParametreServiceImpl implements ParametreService {

    private final ParametreRepository parametreRepository;

    @Override
    public String getValeur(String cle) {
        return parametreRepository.findById(cle)
                .map(Parametre::getValeur)
                .orElse(null);
    }

    @Override
    public String getValeur(String cle, String defaultValue) {
        String val = getValeur(cle);
        return val != null ? val : defaultValue;
    }

    @Override
    public Integer getInt(String cle) {
        String val = getValeur(cle);
        if (val == null) return null;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("Paramètre {} n'est pas un entier valide : {}", cle, val);
            return null;
        }
    }

    @Override
    public Integer getInt(String cle, Integer defaultValue) {
        Integer val = getInt(cle);
        return val != null ? val : defaultValue;
    }

    @Override
    public Long getLong(String cle) {
        String val = getValeur(cle);
        if (val == null) return null;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            log.warn("Paramètre {} n'est pas un Long valide : {}", cle, val);
            return null;
        }
    }

    @Override
    public Long getLong(String cle, Long defaultValue) {
        Long val = getLong(cle);
        return val != null ? val : defaultValue;
    }

    @Override
    public Double getDouble(String cle) {
        String val = getValeur(cle);
        if (val == null) return null;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            log.warn("Paramètre {} n'est pas un double valide : {}", cle, val);
            return null;
        }
    }

    @Override
    public Double getDouble(String cle, Double defaultValue) {
        Double val = getDouble(cle);
        return val != null ? val : defaultValue;
    }

    @Override
    public Boolean getBoolean(String cle) {
        String val = getValeur(cle);
        if (val == null) return null;
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    @Override
    public Boolean getBoolean(String cle, Boolean defaultValue) {
        Boolean val = getBoolean(cle);
        return val != null ? val : defaultValue;
    }

    @Override
    public String getCronBatchTurnover() {
        return getValeur("batch.turnover.cron", "0 0 2 1 * ?");
    }
}