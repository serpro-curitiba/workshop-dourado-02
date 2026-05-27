package br.gov.serpro.sifap.calculo.domain;

import java.math.BigDecimal;

/**
 * Resultado da aplicacao de desconto judicial.
 * {@code capped} indica que o valor solicitado excedia 70% do bruto e foi limitado.
 */
public record JudicialDiscountResult(BigDecimal appliedDiscount, boolean capped, BigDecimal requestedDiscount) {
}
