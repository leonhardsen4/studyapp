package com.leonhardsen.studyapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa um assunto de estudo vinculado a uma {@link Disciplina}.
 * Cada assunto possui um nível de dificuldade, um número mínimo de sessões
 * Pomodoro e um estado de progresso ({@link TipoStatusAssunto}).
 */
public class Assunto {

    private int id;
    private int disciplinaId;
    private String nome;
    private TipoDificuldade dificuldade;
    private int sessoesMinimas;
    private int sessoesRealizadas;
    private TipoStatusAssunto status;
    private LocalDate dataLimite;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    /** Construtor padrão sem argumentos. */
    public Assunto() {}

    /**
     * Cria um novo assunto com status inicial PENDENTE e sem sessões realizadas.
     *
     * @param disciplinaId  identificador da disciplina à qual pertence
     * @param nome          nome do assunto
     * @param dificuldade   nível de dificuldade do assunto
     * @param sessoesMinimas número mínimo de sessões Pomodoro para cobrir o assunto
     */
    public Assunto(int disciplinaId, String nome, TipoDificuldade dificuldade, int sessoesMinimas) {
        this.disciplinaId = disciplinaId;
        this.nome = nome;
        this.dificuldade = dificuldade;
        this.sessoesMinimas = sessoesMinimas;
        this.sessoesRealizadas = 0;
        this.status = TipoStatusAssunto.PENDENTE;
    }

    /** Retorna o identificador único do assunto. */
    public int getId() { return id; }
    /** Define o identificador único do assunto. */
    public void setId(int id) { this.id = id; }

    /** Retorna o identificador da disciplina pai. */
    public int getDisciplinaId() { return disciplinaId; }
    /** Define o identificador da disciplina pai. */
    public void setDisciplinaId(int disciplinaId) { this.disciplinaId = disciplinaId; }

    /** Retorna o nome do assunto. */
    public String getNome() { return nome; }
    /** Define o nome do assunto. */
    public void setNome(String nome) { this.nome = nome; }

    /** Retorna o nível de dificuldade do assunto. */
    public TipoDificuldade getDificuldade() { return dificuldade; }
    /** Define o nível de dificuldade do assunto. */
    public void setDificuldade(TipoDificuldade dificuldade) { this.dificuldade = dificuldade; }

    /** Retorna o número mínimo de sessões Pomodoro definido para o assunto. */
    public int getSessoesMinimas() { return sessoesMinimas; }
    /** Define o número mínimo de sessões Pomodoro para o assunto. */
    public void setSessoesMinimas(int sessoesMinimas) { this.sessoesMinimas = sessoesMinimas; }

    /** Retorna o número de sessões Pomodoro já realizadas para este assunto. */
    public int getSessoesRealizadas() { return sessoesRealizadas; }
    /** Define o número de sessões Pomodoro já realizadas. */
    public void setSessoesRealizadas(int sessoesRealizadas) { this.sessoesRealizadas = sessoesRealizadas; }

    /** Retorna o status de progresso do assunto. */
    public TipoStatusAssunto getStatus() { return status; }
    /** Define o status de progresso do assunto. */
    public void setStatus(TipoStatusAssunto status) { this.status = status; }

    /** Retorna a data limite para estudo deste assunto, ou {@code null} se não definida. */
    public LocalDate getDataLimite() { return dataLimite; }
    /** Define a data limite para estudo deste assunto. */
    public void setDataLimite(LocalDate dataLimite) { this.dataLimite = dataLimite; }

    /** Retorna a data/hora de criação do assunto. */
    public LocalDateTime getCriadoEm() { return criadoEm; }
    /** Define a data/hora de criação do assunto. */
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    /** Retorna a data/hora da última atualização do assunto. */
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    /** Define a data/hora da última atualização do assunto. */
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    @Override
    public String toString() { return nome; }
}
