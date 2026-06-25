package com.leonhardsen.studyapp.model;

/**
 * Enumeração dos possíveis estados de uma {@link Tarefa}.
 *
 * @author StudyApp
 * @version 1.0
 */
public enum TipoStatus {
    PENDENTE("Pendente"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDA("Concluída");

    private final String descricao;

    TipoStatus(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
