package br.gov.serpro.sifap.conciliacao.application;

import br.gov.serpro.sifap.pagamento.application.Cnab240GeneratorService;
import br.gov.serpro.sifap.pagamento.domain.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Cnab240ReturnParserTest {

    private final Cnab240ReturnParser parser = new Cnab240ReturnParser();
    private final Cnab240GeneratorService generator = new Cnab240GeneratorService();

    // requirement: REQ-PAY-008
    @Test
    void parsesReturnDetailRecordsFromGeneratedCnab() {
        UUID paymentId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Payment payment = new Payment(
                "12345678910",
                "SIFAP-BASICO",
                "2026-05",
                new BigDecimal("831.60"),
                new BigDecimal("831.60"),
                LocalDate.of(2026, 5, 7),
                "001",
                "3401",
                "00012345678"
        );
        ReflectionTestUtils.setField(payment, "id", paymentId);
        String content = generator.generate("2026-05", List.of(payment)).content();

        List<Cnab240ReturnParser.ReturnRecord> records = parser.parse(content);

        assertEquals(1, records.size());
        Cnab240ReturnParser.ReturnRecord record = records.getFirst();
        assertEquals(paymentId, record.paymentId());
        assertEquals("001", record.bankCode());
        assertEquals("3401", record.agency());
        assertEquals("00012345678", record.account());
        assertEquals(new BigDecimal("831.60"), record.amount());
        assertEquals(LocalDate.of(2026, 5, 7), record.nominalDate());
    }

    // requirement: REQ-PAY-008
    @Test
    void rejectsBlankReturnFile() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("   "));
    }
}
