package com.leonhardsen.studyapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Representa um evento na agenda de um usuário.
 * Um evento possui título obrigatório, data e horários de início e fim opcionais.
 */
public class Evento {

    private int id;
    private int usuarioId;
    private String titulo;
    private String descricao;
    private LocalDate data;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private LocalDateTime criadoEm;

    /**
     * Construtor padrão. Inicializa a descrição com string vazia.
     */
    public Evento() {
        this.descricao = "";
    }

    /** Retorna o identificador único do evento. */
    public int getId() { return id; }
    /** Define o identificador único do evento. */
    public void setId(int id) { this.id = id; }

    /** Retorna o identificador do usuário proprietário. */
    public int getUsuarioId() { return usuarioId; }
    /** Define o identificador do usuário proprietário. */
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    /** Retorna o título do evento. */
    public String getTitulo() { return titulo; }
    /** Define o título do evento. */
    public void setTitulo(String titulo) { this.titulo = titulo; }

    /** Retorna a descrição do evento, nunca {@code null}. */
    public String getDescricao() { return descricao != null ? descricao : ""; }
    /** Define a descrição do evento; {@code null} é tratado como string vazia. */
    public void setDescricao(String descricao) { this.descricao = descricao != null ? descricao : ""; }

    /** Retorna a data do evento. */
    public LocalDate getData() { return data; }
    /** Define a data do evento. */
    public void setData(LocalDate data) { this.data = data; }

    /** Retorna o horário de início do evento, ou {@code null} se não definido. */
    public LocalTime getHoraInicio() { return horaInicio; }
    /** Define o horário de início do evento. */
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    /** Retorna o horário de fim do evento, ou {@code null} se não definido. */
    public LocalTime getHoraFim() { return horaFim; }
    /** Define o horário de fim do evento. */
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }

    /** Retorna a data/hora de criação do evento. */
    public LocalDateTime getCriadoEm() { return criadoEm; }
    /** Define a data/hora de criação do evento. */
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
