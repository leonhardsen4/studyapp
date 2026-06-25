package com.leonhardsen.studyapp.model;

import java.time.LocalDateTime;

/**
 * Representa uma sessão Pomodoro concluída e registrada pelo sistema.
 * Pode ser uma sessão de foco (vinculada opcionalmente a um {@link Assunto})
 * ou uma pausa (curta ou longa).
 */
public class SessaoPomodoro {

    private int id;
    private int usuarioId;
    private Integer assuntoId;
    private TipoSessao tipo;
    private LocalDateTime iniciadoEm;
    private LocalDateTime concluidoEm;
    private int duracaoSegundos;

    /** Construtor padrão sem argumentos. */
    public SessaoPomodoro() {}

    /**
     * Cria uma sessão Pomodoro com todos os dados necessários para persistência.
     *
     * @param usuarioId       identificador do usuário que realizou a sessão
     * @param assuntoId       identificador do assunto estudado, ou {@code null} para sessões sem assunto
     * @param tipo            tipo da sessão (foco, pausa curta ou pausa longa)
     * @param iniciadoEm      data/hora de início da sessão
     * @param concluidoEm     data/hora de conclusão da sessão
     * @param duracaoSegundos duração efetiva da sessão em segundos
     */
    public SessaoPomodoro(int usuarioId, Integer assuntoId, TipoSessao tipo,
                          LocalDateTime iniciadoEm, LocalDateTime concluidoEm, int duracaoSegundos) {
        this.usuarioId = usuarioId;
        this.assuntoId = assuntoId;
        this.tipo = tipo;
        this.iniciadoEm = iniciadoEm;
        this.concluidoEm = concluidoEm;
        this.duracaoSegundos = duracaoSegundos;
    }

    /** Retorna o identificador único da sessão. */
    public int getId() { return id; }
    /** Define o identificador único da sessão. */
    public void setId(int id) { this.id = id; }

    /** Retorna o identificador do usuário que realizou a sessão. */
    public int getUsuarioId() { return usuarioId; }
    /** Define o identificador do usuário que realizou a sessão. */
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    /** Retorna o identificador do assunto estudado, ou {@code null} se nenhum. */
    public Integer getAssuntoId() { return assuntoId; }
    /** Define o identificador do assunto estudado. */
    public void setAssuntoId(Integer assuntoId) { this.assuntoId = assuntoId; }

    /** Retorna o tipo da sessão (foco, pausa curta ou pausa longa). */
    public TipoSessao getTipo() { return tipo; }
    /** Define o tipo da sessão. */
    public void setTipo(TipoSessao tipo) { this.tipo = tipo; }

    /** Retorna a data/hora em que a sessão foi iniciada. */
    public LocalDateTime getIniciadoEm() { return iniciadoEm; }
    /** Define a data/hora de início da sessão. */
    public void setIniciadoEm(LocalDateTime iniciadoEm) { this.iniciadoEm = iniciadoEm; }

    /** Retorna a data/hora em que a sessão foi concluída. */
    public LocalDateTime getConcluidoEm() { return concluidoEm; }
    /** Define a data/hora de conclusão da sessão. */
    public void setConcluidoEm(LocalDateTime concluidoEm) { this.concluidoEm = concluidoEm; }

    /** Retorna a duração efetiva da sessão em segundos. */
    public int getDuracaoSegundos() { return duracaoSegundos; }
    /** Define a duração efetiva da sessão em segundos. */
    public void setDuracaoSegundos(int duracaoSegundos) { this.duracaoSegundos = duracaoSegundos; }
}
