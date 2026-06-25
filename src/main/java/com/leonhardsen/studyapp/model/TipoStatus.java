package com.leonhardsen.studyapp.model;

/**
 * Enumeração dos possíveis estados de uma {@link Tarefa}.
 *
 * @author StudyApp
 * @version 1.0
 */
public enum TipoStatus {

    /** Tarefa criada mas ainda não iniciada. */
    PENDENTE("Pendente"),

    /** Tarefa em execução — trabalho em progresso. */
    EM_ANDAMENTO("Em andamento"),

    /** Tarefa finalizada com sucesso. */
    CONCLUIDA("Concluída");

    private final String descricao;

    TipoStatus(String descricao) {
        this.descricao = descricao;
    }

    /** Retorna a descrição legível do status. */
    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
