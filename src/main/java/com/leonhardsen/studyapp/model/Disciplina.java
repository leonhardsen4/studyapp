package com.leonhardsen.studyapp.model;

import java.time.LocalDateTime;

/**
 * Representa uma disciplina de estudo pertencente a um usuário.
 * Uma disciplina agrupa um conjunto de {@link Assunto}s relacionados.
 */
public class Disciplina {

    private int id;
    private int usuarioId;
    private String nome;
    private LocalDateTime criadoEm;

    /** Construtor padrão sem argumentos. */
    public Disciplina() {}

    /**
     * Cria uma nova disciplina com os dados essenciais.
     *
     * @param usuarioId identificador do usuário proprietário
     * @param nome      nome da disciplina
     */
    public Disciplina(int usuarioId, String nome) {
        this.usuarioId = usuarioId;
        this.nome = nome;
    }

    /** Retorna o identificador único da disciplina. */
    public int getId() { return id; }
    /** Define o identificador único da disciplina. */
    public void setId(int id) { this.id = id; }

    /** Retorna o identificador do usuário proprietário. */
    public int getUsuarioId() { return usuarioId; }
    /** Define o identificador do usuário proprietário. */
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    /** Retorna o nome da disciplina. */
    public String getNome() { return nome; }
    /** Define o nome da disciplina. */
    public void setNome(String nome) { this.nome = nome; }

    /** Retorna a data/hora de criação da disciplina. */
    public LocalDateTime getCriadoEm() { return criadoEm; }
    /** Define a data/hora de criação da disciplina. */
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    @Override
    public String toString() { return nome; }
}
