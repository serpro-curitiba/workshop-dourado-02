package br.gov.serpro.sifap.pagamento.application;

import br.gov.serpro.sifap.pagamento.domain.Payment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class Cnab240GeneratorService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("ddMMyyyy");

    public CnabFile generate(String competence, List<Payment> payments) {
        StringBuilder content = new StringBuilder();
        content.append(padRight("00100000         2SIFAP 2.0                SERPRO SIFAP                 1", 240)).append('\n');
        BigDecimal total = BigDecimal.ZERO;
        int sequence = 1;
        for (Payment payment : payments) {
            total = total.add(payment.getNetAmount());
            content.append(detailLine(sequence++, payment)).append('\n');
        }
        content.append(trailerLine(payments.size(), total));
        return new CnabFile("CNAB240-" + competence + ".REM", content.toString(), payments.size(), total);
    }

    private static String detailLine(int sequence, Payment payment) {
        String amountInCents = payment.getNetAmount().movePointRight(2).toBigIntegerExact().toString();
        String line = "00100013A000" + padLeft(String.valueOf(sequence), 5, '0')
                + padRight(payment.getBankCode(), 3)
                + padRight(payment.getAgency(), 5)
                + padRight(payment.getAccount(), 12)
                + padLeft(amountInCents, 15, '0')
                + payment.getNominalDate().format(DATE)
                + padRight(onlyAscii("SIFAP BENEFICIARIO " + payment.getBeneficiaryCpf()), 40)
                + padRight(payment.getId().toString(), 36);
        return padRight(line, 240);
    }

    private static String trailerLine(int count, BigDecimal total) {
        String amountInCents = total.movePointRight(2).toBigIntegerExact().toString();
        String line = "00199999         " + padLeft(String.valueOf(count), 6, '0') + padLeft(amountInCents, 18, '0');
        return padRight(line, 240);
    }

    private static String padRight(String value, int size) {
        return padRight(value, size, ' ');
    }

    private static String padRight(String value, int size, char pad) {
        String normalized = value == null ? "" : value;
        if (normalized.length() >= size) {
            return normalized.substring(0, size);
        }
        return normalized + String.valueOf(pad).repeat(size - normalized.length());
    }

    private static String padLeft(String value, int size, char pad) {
        String normalized = value == null ? "" : value;
        if (normalized.length() >= size) {
            return normalized.substring(normalized.length() - size);
        }
        return String.valueOf(pad).repeat(size - normalized.length()) + normalized;
    }

    private static String onlyAscii(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    public record CnabFile(String filename, String content, int recordCount, BigDecimal totalAmount) {
    }
}