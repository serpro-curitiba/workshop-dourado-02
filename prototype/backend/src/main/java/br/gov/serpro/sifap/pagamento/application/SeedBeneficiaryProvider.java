package br.gov.serpro.sifap.pagamento.application;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SeedBeneficiaryProvider {

    public List<SeedBeneficiary> eligibleBeneficiaries() {
        return List.of(
                new SeedBeneficiary("12345678910", "SIFAP-BASICO", "41", 4, 72,
                        new BigDecimal("600.00"), new BigDecimal("1.05"), new BigDecimal("1.20"),
                        new BigDecimal("1.00"), new BigDecimal("1.10"), new BigDecimal("0.00"),
                        true, "001", "3401", "00012345678"),
                new SeedBeneficiary("00098765432", "SIFAP-BASICO", "99", 2, 44,
                        new BigDecimal("600.00"), new BigDecimal("1.15"), new BigDecimal("1.05"),
                        new BigDecimal("0.95"), new BigDecimal("1.00"), new BigDecimal("50.00"),
                        false, "104", "1209", "00098765432"),
                new SeedBeneficiary("55566677788", "SIFAP-JUD", "35", 5, 38,
                        new BigDecimal("500.00"), new BigDecimal("1.00"), new BigDecimal("1.30"),
                        new BigDecimal("1.00"), new BigDecimal("1.00"), new BigDecimal("400.00"),
                        true, "237", "0001", "00055566778")
        );
    }
}