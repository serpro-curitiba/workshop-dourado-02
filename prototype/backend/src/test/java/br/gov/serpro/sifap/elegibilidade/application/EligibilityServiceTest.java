package br.gov.serpro.sifap.elegibilidade.application;

import br.gov.serpro.sifap.elegibilidade.domain.EligibilityDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de EligibilityService.
 *
 * REQ-PAY-004 (BR-003 / MYS-004): bypass de regiao 99 com auditoria explicita.
 * REQ-PAY-005 (BR-012 / MYS-003): bypass de prefixo CPF com auditoria explicita.
 */
class EligibilityServiceTest {

    private final EligibilityService service = new EligibilityService("000", "99");

    // requirement: REQ-PAY-005
    @Test
    void cpfPrefixBypassAllowsEvenWithoutDocs() {
        EligibilityDecision d = service.evaluateDocuments("00012345678", false);
        assertTrue(d.eligible());
        assertNotNull(d.bypassReason(), "deve indicar bypass para auditoria");
        assertTrue(d.bypassReason().startsWith("CPF_PREFIX_BYPASS"));
    }

    // requirement: REQ-PAY-005
    @Test
    void normalCpfWithoutDocsIsDenied() {
        EligibilityDecision d = service.evaluateDocuments("12345678901", false);
        assertFalse(d.eligible());
        assertNull(d.bypassReason());
    }

    // requirement: REQ-PAY-005
    @Test
    void normalCpfWithDocsIsAllowedWithoutBypass() {
        EligibilityDecision d = service.evaluateDocuments("12345678901", true);
        assertTrue(d.eligible());
        assertNull(d.bypassReason(), "fluxo normal nao registra bypass");
    }

    // requirement: REQ-PAY-005
    @Test
    void nullCpfIsDenied() {
        assertFalse(service.evaluateDocuments(null, true).eligible());
        assertFalse(service.evaluateDocuments("12", true).eligible());
    }

    // requirement: REQ-PAY-004
    @Test
    void regionBypassDetectsRegion99() {
        assertTrue(service.isRegionBypass("99"));
        assertFalse(service.isRegionBypass("SP"));
        assertFalse(service.isRegionBypass(null));
    }
}
