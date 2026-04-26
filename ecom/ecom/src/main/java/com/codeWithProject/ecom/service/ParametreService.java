package com.codeWithProject.ecom.service;

public interface ParametreService {

    String getValeur(String cle);

    String getValeur(String cle, String defaultValue);

    Integer getInt(String cle);

    Integer getInt(String cle, Integer defaultValue);

    Long getLong(String cle);

    Long getLong(String cle, Long defaultValue);

    Double getDouble(String cle);

    Double getDouble(String cle, Double defaultValue);

    Boolean getBoolean(String cle);

    Boolean getBoolean(String cle, Boolean defaultValue);

    String getCronBatchTurnover();
}