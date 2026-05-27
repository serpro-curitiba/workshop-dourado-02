package br.gov.serpro.sifap.pagamento.application;

import br.gov.serpro.sifap.shared.security.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CpfMaskingServiceTest {

    private final CpfMaskingService service = new CpfMaskingService();

    // requirement: REQ-PAY-014
    @Test
    void returnsFullCpfForAuditAndAdminRoles() {
        assertEquals("12345678910", service.maskForRole("12345678910", Role.ADM));
        assertEquals("12345678910", service.maskForRole("12345678910", Role.AUD));
    }

    // requirement: REQ-PAY-014
    @Test
    void masksCpfForOperatorRole() {
        assertEquals("***.***.789-**", service.maskForRole("12345678910", Role.OPR));
    }

    // requirement: REQ-PAY-014
    @Test
    void returnsSafeMaskForInvalidCpf() {
        assertEquals("***.***.***-**", service.maskForRole("ABC", Role.OPR));
    }
}
