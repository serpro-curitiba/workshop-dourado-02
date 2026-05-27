package br.gov.serpro.sifap.pagamento.application;

import br.gov.serpro.sifap.pagamento.domain.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Cnab240GeneratorServiceTest {

    private final Cnab240GeneratorService service = new Cnab240GeneratorService();

    // requirement: REQ-PAY-006
    @Test
    void generatesFixedWidthCnabWithTotals() {
        Payment first = payment(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "12345678910",
                new BigDecimal("831.60"),
                "001",
                "3401",
                "00012345678"
        );
        Payment second = payment(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "55566677788",
                new BigDecimal("250.00"),
                "237",
                "0001",
                "00055566778"
        );

        Cnab240GeneratorService.CnabFile file = service.generate("2026-05", List.of(first, second));

        assertEquals("CNAB240-2026-05.REM", file.filename());
        assertEquals(2, file.recordCount());
        assertEquals(new BigDecimal("1081.60"), file.totalAmount());

        String[] lines = file.content().split("\\R");
        assertEquals(4, lines.length);
        for (String line : lines) {
            assertEquals(240, line.length());
        }
        assertTrue(lines[1].startsWith("00100013A000000010013401 00012345678 00000000008316007052026"));
        assertTrue(lines[3].startsWith("00199999         000002000000000000108160"));
    }

    private static Payment payment(UUID id, String cpf, BigDecimal amount, String bankCode, String agency, String account) {
        Payment payment = new Payment(
                cpf,
                "SIFAP-BASICO",
                "2026-05",
                amount,
                amount,
                LocalDate.of(2026, 5, 7),
                bankCode,
                agency,
                account
        );
        ReflectionTestUtils.setField(payment, "id", id);
        return payment;
    }
}
