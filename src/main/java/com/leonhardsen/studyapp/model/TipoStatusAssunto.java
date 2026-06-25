package com.leonhardsen.studyapp.model;

/**
 * Enumeração dos possíveis estados de progresso de um {@link Assunto}.
 * O ciclo de vida normal segue a ordem: PENDENTE → EM_ANDAMENTO → CONCLUIDO.
 */
public enum TipoStatusAssunto {

    /** Assunto ainda não iniciado — nenhuma sessão registrada. */
    PENDENTE("Pendente"),

    /** Assunto com pelo menos uma sessão realizada, mas ainda não concluído. */
    EM_ANDAMENTO("Em Andamento"),

    /** Assunto marcado como concluído pelo usuário. */
    CONCLUIDO("Concluído");

    private final String label;

    TipoStatusAssunto(String label) { this.label = label; }

    /** Retorna o rótulo legível do status do assunto. */
    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
