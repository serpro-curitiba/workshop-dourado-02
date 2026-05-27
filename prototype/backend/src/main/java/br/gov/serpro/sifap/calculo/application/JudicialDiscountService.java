package br.gov.serpro.sifap.calculo.application;

import br.gov.serpro.sifap.calculo.domain.JudicialDiscountResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Aplica desconto judicial com teto regulatorio.
 *
 * REQ-PAY-012 / BR-019 (resolve MYS-010): legado CALCDSCT.NSN nao tinha cap,
 * permitindo bloqueio integral do beneficio (risco fiscal). Aqui aplicamos
 * limite de 70% do bruto e emitimos sinal {@code capped=true} para que a
 * camada de aplicacao registre evento JUDICIAL_DISCOUNT_CAPPED em audit_event.
 *
 * source_legacy: 01-arqueologia/legado-sifap/natural-programs/CALCDSCT.NSN#L60-L95
 */
@Service
public class JudicialDiscountService {

    /** Teto regulatorio: 70% do valor bruto. */
    private static final BigDecimal CAP_RATIO = new BigDecimal("0.70");

    public JudicialDiscountResult apply(BigDecimal grossAmount, BigDecimal requestedDiscount) {
        if (grossAmount == null || requestedDiscount == null) {
            throw new IllegalArgumentException("grossAmount e requestedDiscount sao obrigatorios");
        }
        if (grossAmount.signum() <= 0) {
            throw new IllegalArgumentException("grossAmount deve ser positivo");
        }
        if (requestedDiscount.signum() < 0) {
            throw new IllegalArgumentException("requestedDiscount nao pode ser negativo");
        }

        BigDecimal cap = grossAmount.multiply(CAP_RATIO).setScale(2, RoundingMode.HALF_UP);
        if (requestedDiscount.compareTo(cap) > 0) {
            return new JudicialDiscountResult(cap, true, requestedDiscount);
        }
        return new JudicialDiscountResult(
                requestedDiscount.setScale(2, RoundingMode.HALF_UP), false, requestedDiscount);
    }
}
