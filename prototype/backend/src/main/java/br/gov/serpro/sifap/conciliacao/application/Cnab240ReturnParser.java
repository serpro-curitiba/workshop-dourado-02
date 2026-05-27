package br.gov.serpro.sifap.conciliacao.application;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class Cnab240ReturnParser {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("ddMMyyyy");

    public List<ReturnRecord> parse(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("arquivo CNAB nao pode ser vazio");
        }
        return Arrays.stream(content.split("\\R"))
                .filter(line -> line.length() >= 136)
                .filter(line -> line.startsWith("00100013A"))
                .map(this::parseDetail)
                .toList();
    }

    private ReturnRecord parseDetail(String line) {
        String bankCode = line.substring(17, 20).trim();
        String agency = line.substring(20, 25).trim();
        String account = line.substring(25, 37).trim();
        BigDecimal amount = new BigDecimal(line.substring(37, 52).trim()).movePointLeft(2);
        LocalDate nominalDate = LocalDate.parse(line.substring(52, 60), DATE);
        UUID paymentId = UUID.fromString(line.substring(100, 136).trim());
        return new ReturnRecord(paymentId, bankCode, agency, account, amount, nominalDate);
    }

    public record ReturnRecord(UUID paymentId, String bankCode, String agency, String account,
                               BigDecimal amount, LocalDate nominalDate) {
    }
}