package br.gov.serpro.sifap.pagamento.application;

import br.gov.serpro.sifap.shared.security.Role;
import org.springframework.stereotype.Service;

@Service
public class CpfMaskingService {

    public String maskForRole(String cpf, Role role) {
        if (role == Role.ADM || role == Role.AUD) {
            return cpf;
        }
        String digits = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return "***.***.***-**";
        }
        return "***.***." + digits.substring(6, 9) + "-**";
    }
}