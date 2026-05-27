package br.gov.serpro.sifap.pagamento.application;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes da regra de calendario do ciclo.
 *
 * REQ-PAY-001 (BR-007): 5o dia util.
 * REQ-PAY-011 (BR-010): regime de dezembro (dia 20).
 */
class CycleCalendarTest {

    // requirement: REQ-PAY-001
    @Test
    void fifthBusinessDayJune2026() {
        // junho/2026: 1=seg, 2=ter, 3=qua, 4=qui, 5=sex -> 5o util = 5/jun
        assertEquals(LocalDate.of(2026, 6, 5), CycleCalendar.fifthBusinessDay(2026, 6));
    }

    // requirement: REQ-PAY-001
    @Test
    void fifthBusinessDaySkipsWeekend() {
        // novembro/2026: 1=dom, 2=seg, 3=ter(feriado civil ignorado por ora),
        // contando so sab/dom: 2,3,4,5,6 = 5 dias uteis -> 6/nov
        assertEquals(LocalDate.of(2026, 11, 6), CycleCalendar.fifthBusinessDay(2026, 11));
    }

    // requirement: REQ-PAY-011
    @Test
    void decemberUsesDay20() {
        assertEquals(LocalDate.of(2026, 12, 20), CycleCalendar.nominalDate(2026, 12));
    }

    // requirement: REQ-PAY-011
    @Test
    void nonDecemberUsesFifthBusinessDay() {
        assertEquals(CycleCalendar.fifthBusinessDay(2026, 6), CycleCalendar.nominalDate(2026, 6));
    }
}
