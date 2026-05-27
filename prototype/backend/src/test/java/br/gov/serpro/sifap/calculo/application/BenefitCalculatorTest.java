package br.gov.serpro.sifap.calculo.application;

import br.gov.serpro.sifap.calculo.domain.CalculationContext;
import br.gov.serpro.sifap.calculo.domain.CalculationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes do BenefitCalculator.
 *
 * REQ-PAY-002 (BR-001): formula BASE x FAT_REG x FAT_FAM x FAT_RND x FAT_IDADE.
 * REQ-PAY-003 (BR-013): arredondamento HALF_UP escala 2.
 */
class BenefitCalculatorTest {

    private final BenefitCalculator calculator = new BenefitCalculator();

    // requirement: REQ-PAY-002
    @Test
    void computesGrossUsingFiveFactorFormula() {
        var ctx = new CalculationContext(
                "12345678901", "SP", 3, 40,
                new BigDecimal("600.00"),
                new BigDecimal("1.05"),
                new BigDecimal("1.20"),
                new BigDecimal("1.00"),
                new BigDecimal("1.10")
        );

        CalculationResult result = calculator.compute(ctx);

        // 600.00 * 1.05 * 1.20 * 1.00 * 1.10 = 831.60
        assertEquals(new BigDecimal("831.60"), result.grossAmount());
    }

    // requirement: REQ-PAY-003
    @Test
    void roundsHalfUpAtScaleTwo() {
        // Forca produto = 100.505 -> arredondar para 100.51
        var ctx = new CalculationContext(
                "12345678901", "SP", 1, 30,
                new BigDecimal("100.505"),
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE
        );
        assertEquals(new BigDecimal("100.51"), calculator.compute(ctx).grossAmount());
    }

    // requirement: REQ-PAY-002
    @Test
    void zeroFactorYieldsZeroGross() {
        var ctx = new CalculationContext(
                "12345678901", "SP", 1, 30,
                new BigDecimal("600.00"),
                BigDecimal.ZERO,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE
        );
        assertEquals(new BigDecimal("0.00"), calculator.compute(ctx).grossAmount());
    }
}
