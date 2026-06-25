package com.leonhardsen.studyapp.model;

/**
 * Enumeração dos tipos de sessão do temporizador Pomodoro.
 * Define as três fases do ciclo: foco, pausa curta e pausa longa.
 */
public enum TipoSessao {

    /** Sessão de foco — período de estudo concentrado. */
    FOCO("Foco"),

    /** Pausa curta entre sessões de foco. */
    PAUSA_CURTA("Pausa Curta"),

    /** Pausa longa realizada após completar um ciclo de quatro sessões de foco. */
    PAUSA_LONGA("Pausa Longa");

    private final String label;

    TipoSessao(String label) { this.label = label; }

    /** Retorna o rótulo legível do tipo de sessão. */
    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
