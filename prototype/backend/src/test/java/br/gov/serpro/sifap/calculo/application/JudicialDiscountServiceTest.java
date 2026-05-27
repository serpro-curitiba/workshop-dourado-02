package br.gov.serpro.sifap.calculo.application;

import br.gov.serpro.sifap.calculo.domain.JudicialDiscountResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de JudicialDiscountService.
 *
 * REQ-PAY-012 (BR-019 / MYS-010): teto de 70% do bruto, sinaliza cap aplicado.
 * Cenarios derivados diretamente do acceptance criteria da spec.
 */
class JudicialDiscountServiceTest {

    private final JudicialDiscountService service = new JudicialDiscountService();

    // requirement: REQ-PAY-012
    @Test
    void discountBelowCapIsAppliedAsIs() {
        // bruto 500, desconto 200 (40%) -> aplica 200, sem cap
        JudicialDiscountResult r = service.apply(new BigDecimal("500.00"), new BigDecimal("200.00"));
        assertEquals(0, r.appliedDiscount().compareTo(new BigDecimal("200.00")));
        assertFalse(r.capped());
    }

    // requirement: REQ-PAY-012
    @Test
    void discountAboveCapIsLimitedTo70Percent() {
        // bruto 500, desconto 400 (80%) -> aplica 350 (70%) e sinaliza cap
        JudicialDiscountResult r = service.apply(new BigDecimal("500.00"), new BigDecimal("400.00"));
        assertEquals(0, r.appliedDiscount().compareTo(new BigDecimal("350.00")));
        assertTrue(r.capped(), "deve sinalizar cap para audit_event JUDICIAL_DISCOUNT_CAPPED");
        assertEquals(0, r.requestedDiscount().compareTo(new BigDecimal("400.00")));
    }

    // requirement: REQ-PAY-012
    @Test
    void discountExactlyAtCapIsNotFlagged() {
        // bruto 1000, desconto 700 (70%) -> aplica 700, sem cap
        JudicialDiscountResult r = service.apply(new BigDecimal("1000.00"), new BigDecimal("700.00"));
        assertEquals(0, r.appliedDiscount().compareTo(new BigDecimal("700.00")));
        assertFalse(r.capped());
    }

    // requirement: REQ-PAY-012
    @Test
    void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.apply(null, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class,
                () -> service.apply(BigDecimal.TEN, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.apply(BigDecimal.ZERO, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class,
                () -> service.apply(BigDecimal.TEN, new BigDecimal("-1")));
    }
}
