package br.gov.serpro.sifap.pagamento.application;

import java.math.BigDecimal;

public record SeedBeneficiary(
        String cpf,
        String programCode,
        String regionCode,
        int familySize,
        int ageYears,
        BigDecimal baseAmount,
        BigDecimal regionalFactor,
        BigDecimal familyFactor,
        BigDecimal incomeFactor,
        BigDecimal ageFactor,
        BigDecimal requestedJudicialDiscount,
        boolean documentsOk,
        String bankCode,
        String agency,
        String account
) {
}