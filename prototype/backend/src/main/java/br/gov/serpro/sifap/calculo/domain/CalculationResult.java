package br.gov.serpro.sifap.calculo.domain;

import java.math.BigDecimal;

public record CalculationResult(BigDecimal grossAmount, BigDecimal netAmount) {}
