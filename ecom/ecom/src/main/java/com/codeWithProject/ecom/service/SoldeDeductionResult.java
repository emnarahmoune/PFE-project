package com.codeWithProject.ecom.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SoldeDeductionResult {
    private int joursDeduits;
    private int joursNonCouverts;
    private int nouveauSolde;
    private boolean deductionEffectuee;

    public static SoldeDeductionResult succes(int joursDeduits, int nouveauSolde) {
        return new SoldeDeductionResult(joursDeduits, 0, nouveauSolde, true);
    }

    public static SoldeDeductionResult partiel(int joursDeduits, int joursNonCouverts, int nouveauSolde) {
        return new SoldeDeductionResult(joursDeduits, joursNonCouverts, nouveauSolde, true);
    }

    public static SoldeDeductionResult echec() {
        return new SoldeDeductionResult(0, 0, 0, false);
    }
}