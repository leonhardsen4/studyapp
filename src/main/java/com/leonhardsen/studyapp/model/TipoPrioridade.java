package com.leonhardsen.studyapp.model;

/**
 * Enumeração dos níveis de prioridade de uma {@link Tarefa}.
 * Os valores estão ordenados do menor para o maior grau de urgência.
 */
public enum TipoPrioridade {

    /** Prioridade baixa — tarefa que pode aguardar. */
    BAIXA("Baixa"),

    /** Prioridade média — tarefa com importância moderada. */
    MEDIA("Média"),

    /** Prioridade alta — tarefa importante que deve ser feita em breve. */
    ALTA("Alta"),

    /** Prioridade urgente — tarefa crítica que requer atenção imediata. */
    URGENTE("Urgente");

    private final String descricao;

    TipoPrioridade(String descricao) {
        this.descricao = descricao;
    }

    /** Retorna a descrição legível do nível de prioridade. */
    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
