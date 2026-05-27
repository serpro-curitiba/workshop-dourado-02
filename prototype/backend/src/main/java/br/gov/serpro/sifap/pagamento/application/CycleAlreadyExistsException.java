package br.gov.serpro.sifap.pagamento.application;

public class CycleAlreadyExistsException extends RuntimeException {
    public CycleAlreadyExistsException(String competence) {
        super("Ciclo ja existe para competencia " + competence);
    }
}
