package com.leonhardsen.studyapp.model;

/**
 * Enumeração dos níveis de dificuldade de um {@link Assunto}.
 * Cada nível carrega um rótulo para exibição e uma sugestão de número mínimo
 * de sessões Pomodoro para o estudo do assunto.
 */
public enum TipoDificuldade {

    /** Assunto considerado fácil; sugestão de 2 sessões. */
    FACIL("Fácil", 2),

    /** Assunto de dificuldade média; sugestão de 4 sessões. */
    MEDIO("Médio", 4),

    /** Assunto difícil; sugestão de 6 sessões. */
    DIFICIL("Difícil", 6),

    /** Assunto muito difícil; sugestão de 8 sessões. */
    MUITO_DIFICIL("Muito Difícil", 8);

    private final String label;
    private final int sessoesDefault;

    TipoDificuldade(String label, int sessoesDefault) {
        this.label = label;
        this.sessoesDefault = sessoesDefault;
    }

    /** Retorna o rótulo legível do nível de dificuldade. */
    public String getLabel() { return label; }

    /** Retorna o número sugerido de sessões Pomodoro para este nível. */
    public int getSessoesDefault() { return sessoesDefault; }

    @Override
    public String toString() { return label; }
}
