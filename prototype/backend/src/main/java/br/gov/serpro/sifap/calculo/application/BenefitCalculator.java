package br.gov.serpro.sifap.calculo.application;

import br.gov.serpro.sifap.calculo.domain.CalculationContext;
import br.gov.serpro.sifap.calculo.domain.CalculationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculo de beneficio.
 *
 * Implementa:
 *   REQ-PAY-002 (BR-001): formula BASE x FAT_REG x FAT_FAM x FAT_RND x FAT_IDADE.
 *   REQ-PAY-003 (BR-013): arredondamento HALF_UP escala 2.
 *
 * source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L186-L255
 */
@Service
public class BenefitCalculator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    public CalculationResult compute(CalculationContext ctx) {
        BigDecimal gross = ctx.baseAmount()
                .multiply(ctx.regionalFactor())
                .multiply(ctx.familyFactor())
                .multiply(ctx.incomeFactor())
                .multiply(ctx.ageFactor())
                .setScale(MONEY_SCALE, MONEY_ROUNDING);
        // descontos virao em iteracao seguinte (REQ-PAY-012)
        return new CalculationResult(gross, gross);
    }
}
