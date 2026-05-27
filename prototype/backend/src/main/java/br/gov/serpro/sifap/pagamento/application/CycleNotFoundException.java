package br.gov.serpro.sifap.pagamento.application;

public class CycleNotFoundException extends RuntimeException {
    public CycleNotFoundException(String competence) {
        super("Pagamentos nao encontrados para competencia " + competence);
    }
}