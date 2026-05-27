package br.gov.serpro.sifap.calculo.domain;

import java.math.BigDecimal;

/**
 * Entrada para calculo de beneficio.
 *
 * source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L186-L255
 * business_rule: BR-001
 */
public record CalculationContext(
        String beneficiaryCpf,
        String regionCode,
        int familySize,
        int ageYears,
        BigDecimal baseAmount,
        BigDecimal regionalFactor,
        BigDecimal familyFactor,
        BigDecimal incomeFactor,
        BigDecimal ageFactor
) {}
