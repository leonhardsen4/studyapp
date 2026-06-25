package com.leonhardsen.studyapp.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utilitário para formatação de datas e horários no padrão brasileiro.
 * Todos os formatadores utilizam {@code Locale.forLanguageTag("pt-BR")} para garantir
 * nomes de dias da semana e meses em português.
 *
 * @author StudyApp
 * @version 1.0
 */
public class FormatadorData {

    /** Locale para o idioma português do Brasil. */
    public static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");

    /** Formatador de hora no padrão HH:mm:ss (ex.: 14:32:05). */
    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss", LOCALE_BR);

    /** Formatador de data curta no padrão dd/MM/yyyy (ex.: 21/06/2026). */
    public static final DateTimeFormatter DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_BR);

    /** Formatador de data longa com dia da semana (ex.: Sábado, 21/06/2026). */
    public static final DateTimeFormatter DATA_LONGA = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", LOCALE_BR);

    /** Formatador de data e hora (ex.: 21/06/2026 14:32). */
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", LOCALE_BR);

    private static final DateTimeFormatter MES_ANO = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_BR);

    /**
     * Construtor privado — classe utilitária, não deve ser instanciada.
     */
    private FormatadorData() {
    }

    /**
     * Formata um horário para o padrão HH:mm:ss.
     *
     * @param hora objeto {@link LocalTime} a ser formatado
     * @return string com o horário formatado
     */
    public static String formatarHora(LocalTime hora) {
        return hora.format(HORA);
    }

    /**
     * Formata uma data para o padrão dd/MM/yyyy.
     *
     * @param data objeto {@link LocalDate} a ser formatado
     * @return string com a data formatada
     */
    public static String formatarDataCurta(LocalDate data) {
        return data.format(DATA_CURTA);
    }

    /**
     * Formata uma data para o padrão longo com dia da semana (ex.: Sábado, 21/06/2026).
     *
     * @param data objeto {@link LocalDate} a ser formatado
     * @return string com a data formatada
     */
    public static String formatarDataLonga(LocalDate data) {
        String formatada = data.format(DATA_LONGA);
        // Capitaliza a primeira letra do dia da semana
        return formatada.substring(0, 1).toUpperCase(LOCALE_BR) + formatada.substring(1);
    }

    /**
     * Formata uma data e hora para o padrão dd/MM/yyyy HH:mm.
     *
     * @param dataHora objeto {@link LocalDateTime} a ser formatado
     * @return string com a data e hora formatadas
     */
    public static String formatarDataHora(LocalDateTime dataHora) {
        return dataHora.format(DATA_HORA);
    }

    /**
     * Formata um mês/ano para exibição no cabeçalho da agenda (ex.: "Junho 2026").
     *
     * @param mesAno objeto {@link YearMonth} a ser formatado
     * @return string com o mês e ano formatados e com primeira letra maiúscula
     */
    public static String formatarMesAno(YearMonth mesAno) {
        String raw = mesAno.format(MES_ANO);
        return raw.substring(0, 1).toUpperCase(LOCALE_BR) + raw.substring(1);
    }
}
