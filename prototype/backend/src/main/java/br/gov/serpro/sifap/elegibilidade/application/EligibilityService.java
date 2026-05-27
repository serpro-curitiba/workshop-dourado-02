package br.gov.serpro.sifap.elegibilidade.application;

import br.gov.serpro.sifap.elegibilidade.domain.EligibilityDecision;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Servico de elegibilidade com tabelas explicitas de excecao.
 *
 * Resolve mistérios da arqueologia tornando as regras-bypass auditaveis:
 *   MYS-003 / BR-012 / REQ-PAY-005: CPF com prefixo legado-bypass.
 *   MYS-004 / BR-003 / REQ-PAY-004: regiao 99 (legado-bypass de fator regional).
 *
 * source_legacy:
 *   01-arqueologia/legado-sifap/natural-programs/VALDOCS.NSN#L88-L120 (CPF prefix)
 *   01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L195-L210 (region 99)
 */
@Service
public class EligibilityService {

    /** Prefixos de CPF que ignoram validacao de documentos (legado-bypass auditavel). */
    private final Set<String> cpfPrefixBypass;
    /** Codigos de regiao que aplicam fator regional 1.0 (legado-bypass auditavel). */
    private final Set<String> regionBypass;

    public EligibilityService(
            @Value("${sifap.eligibility.cpf-prefix-bypass:000}") String cpfPrefixes,
            @Value("${sifap.eligibility.region-bypass:99}") String regions) {
        this.cpfPrefixBypass = parseCsv(cpfPrefixes);
        this.regionBypass = parseCsv(regions);
    }

    /** REQ-PAY-005 / BR-012: bypass de validacao de documentos por prefixo de CPF. */
    public EligibilityDecision evaluateDocuments(String cpf, boolean documentsOk) {
        if (cpf == null || cpf.length() < 3) {
            return EligibilityDecision.deny("CPF invalido");
        }
        String prefix = cpf.substring(0, 3);
        if (cpfPrefixBypass.contains(prefix)) {
            return EligibilityDecision.allowWithBypass(
                    "CPF_PREFIX_BYPASS: prefixo " + prefix + " esta na lista de excecao");
        }
        if (!documentsOk) {
            return EligibilityDecision.deny("Documentacao ausente ou invalida");
        }
        return EligibilityDecision.allow();
    }

    /** REQ-PAY-004 / BR-003: regiao em lista de excecao -> fator regional 1.0 com auditoria. */
    public boolean isRegionBypass(String regionCode) {
        return regionCode != null && regionBypass.contains(regionCode);
    }

    private static Set<String> parseCsv(String csv) {
        return Set.of(csv.split("\\s*,\\s*"));
    }
}
