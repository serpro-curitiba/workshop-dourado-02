package br.gov.serpro.sifap.pagamento.application;

public class CycleAlreadyExistsException extends RuntimeException {
    public CycleAlreadyExistsException(String competence) {
        super("Pagamentos ja existem para competencia " + competence);
    }
}
