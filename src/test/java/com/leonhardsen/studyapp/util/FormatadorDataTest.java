package com.leonhardsen.studyapp.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para {@link FormatadorData}.
 *
 * @author StudyApp
 */
class FormatadorDataTest {

    @Test
    void formatarHora_formatoCorreto() {
        LocalTime hora = LocalTime.of(14, 32, 5);
        assertEquals("14:32:05", FormatadorData.formatarHora(hora));
    }

    @Test
    void formatarHora_meiaDia() {
        assertEquals("12:00:00", FormatadorData.formatarHora(LocalTime.NOON));
    }

    @Test
    void formatarHora_meianoite() {
        assertEquals("00:00:00", FormatadorData.formatarHora(LocalTime.MIDNIGHT));
    }

    @Test
    void formatarDataCurta_formatoCorreto() {
        LocalDate data = LocalDate.of(2026, 6, 21);
        assertEquals("21/06/2026", FormatadorData.formatarDataCurta(data));
    }

    @Test
    void formatarDataCurta_primeiroDia() {
        assertEquals("01/01/2000", FormatadorData.formatarDataCurta(LocalDate.of(2000, 1, 1)));
    }

    @Test
    void formatarDataLonga_capitalizado() {
        LocalDate sabado = LocalDate.of(2026, 6, 20);
        String resultado = FormatadorData.formatarDataLonga(sabado);
        // Primeira letra deve ser maiúscula
        assertTrue(Character.isUpperCase(resultado.charAt(0)));
    }

    @Test
    void formatarDataLonga_contemDiaMesAno() {
        LocalDate data = LocalDate.of(2026, 6, 21);
        String resultado = FormatadorData.formatarDataLonga(data);
        assertTrue(resultado.contains("21/06/2026"), "Deve conter a data formatada");
        assertTrue(resultado.length() > 10, "Deve ter o dia da semana também");
    }

    @Test
    void formatarDataLonga_domingo() {
        // 21/06/2026 é domingo
        LocalDate domingo = LocalDate.of(2026, 6, 21);
        String resultado = FormatadorData.formatarDataLonga(domingo);
        assertTrue(resultado.toLowerCase().contains("domingo"));
    }

    @Test
    void formatarDataHora_formatoCorreto() {
        LocalDateTime dataHora = LocalDateTime.of(2026, 6, 21, 9, 5);
        assertEquals("21/06/2026 09:05", FormatadorData.formatarDataHora(dataHora));
    }

    @Test
    void formatarDataHora_meiaNoite() {
        LocalDateTime dataHora = LocalDateTime.of(2026, 1, 1, 0, 0);
        assertEquals("01/01/2026 00:00", FormatadorData.formatarDataHora(dataHora));
    }
}
