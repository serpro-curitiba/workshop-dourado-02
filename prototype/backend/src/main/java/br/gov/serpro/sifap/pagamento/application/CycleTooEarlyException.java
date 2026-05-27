package br.gov.serpro.sifap.pagamento.application;

import java.time.LocalDate;

public class CycleTooEarlyException extends RuntimeException {
    private final LocalDate earliest;
    private final LocalDate today;

    public CycleTooEarlyException(String competence, LocalDate earliest, LocalDate today) {
        super("Pagamentos da competencia " + competence + " so podem ser abertos a partir de " + earliest + " (hoje=" + today + ")");
        this.earliest = earliest;
        this.today = today;
    }

    public LocalDate getEarliest() { return earliest; }
    public LocalDate getToday() { return today; }
}
