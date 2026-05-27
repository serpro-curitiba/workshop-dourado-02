package br.gov.serpro.sifap.pagamento.application;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Regras de data dos pagamentos.
 *
 * Implementa:
 *   REQ-PAY-001 (BR-007): geracao no 5o dia util.
 *   REQ-PAY-011 (BR-010): em dezembro, data nominal = dia 20.
 *
 * source_legacy: 01-arqueologia/legado-sifap/natural-programs/BATCHPGT.NSN#L42-L115
 */
public final class CycleCalendar {

    private CycleCalendar() {}

    public static LocalDate fifthBusinessDay(int year, int month) {
        LocalDate day = LocalDate.of(year, month, 1);
        int businessDays = 0;
        while (businessDays < 5) {
            if (isBusinessDay(day)) {
                businessDays++;
                if (businessDays == 5) return day;
            }
            day = day.plusDays(1);
        }
        return day;
    }

    public static LocalDate nominalDate(int year, int month) {
        if (month == 12) return LocalDate.of(year, 12, 20); // REQ-PAY-011 / BR-010
        return fifthBusinessDay(year, month);
    }

    private static boolean isBusinessDay(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
        // feriados nacionais serao injetados via HolidayProvider em iteracao seguinte
    }
}
