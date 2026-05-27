package br.gov.serpro.sifap.elegibilidade.domain;

/**
 * Resultado de uma decisao de elegibilidade.
 * Quando bypassReason != null, indica que houve excecao aplicada (exige auditoria).
 */
public record EligibilityDecision(boolean eligible, String reason, String bypassReason) {

    public static EligibilityDecision allow() {
        return new EligibilityDecision(true, null, null);
    }

    public static EligibilityDecision allowWithBypass(String bypassReason) {
        return new EligibilityDecision(true, null, bypassReason);
    }

    public static EligibilityDecision deny(String reason) {
        return new EligibilityDecision(false, reason, null);
    }
}
