package com.leonhardsen.studyapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa uma tarefa criada por um usuário.
 * Cada tarefa deve ter ao menos uma {@link Etiqueta} e no máximo três.
 * Possui prioridade, status de conclusão e data de vencimento opcionais.
 */
public class Tarefa {
    private int id;
    private int usuarioId;
    private String titulo;
    private String anotacoes;
    private TipoPrioridade prioridade;
    private TipoStatus status;
    private LocalDate dataVencimento;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private List<Etiqueta> etiquetas = new ArrayList<>();

    /** Construtor padrão sem argumentos. */
    public Tarefa() {}

    /** Retorna o identificador único da tarefa. */
    public int getId() { return id; }
    /** Define o identificador único da tarefa. */
    public void setId(int id) { this.id = id; }
    /** Retorna o identificador do usuário proprietário. */
    public int getUsuarioId() { return usuarioId; }
    /** Define o identificador do usuário proprietário. */
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    /** Retorna o título da tarefa. */
    public String getTitulo() { return titulo; }
    /** Define o título da tarefa. */
    public void setTitulo(String titulo) { this.titulo = titulo; }
    /** Retorna as anotações da tarefa, nunca {@code null}. */
    public String getAnotacoes() { return anotacoes != null ? anotacoes : ""; }
    /** Define as anotações da tarefa. */
    public void setAnotacoes(String anotacoes) { this.anotacoes = anotacoes; }
    /** Retorna o nível de prioridade da tarefa. */
    public TipoPrioridade getPrioridade() { return prioridade; }
    /** Define o nível de prioridade da tarefa. */
    public void setPrioridade(TipoPrioridade prioridade) { this.prioridade = prioridade; }
    /** Retorna o status de conclusão da tarefa. */
    public TipoStatus getStatus() { return status; }
    /** Define o status de conclusão da tarefa. */
    public void setStatus(TipoStatus status) { this.status = status; }
    /** Retorna a data de vencimento da tarefa, ou {@code null} se não definida. */
    public LocalDate getDataVencimento() { return dataVencimento; }
    /** Define a data de vencimento da tarefa. */
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }
    /** Retorna a data/hora de criação da tarefa. */
    public LocalDateTime getCriadoEm() { return criadoEm; }
    /** Define a data/hora de criação da tarefa. */
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    /** Retorna a data/hora da última atualização da tarefa. */
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    /** Define a data/hora da última atualização da tarefa. */
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    /** Retorna a lista de etiquetas vinculadas à tarefa. */
    public List<Etiqueta> getEtiquetas() { return etiquetas; }
    /**
     * Define a lista de etiquetas da tarefa.
     *
     * @param etiquetas lista de etiquetas; se {@code null}, é substituída por lista vazia
     */
    public void setEtiquetas(List<Etiqueta> etiquetas) { this.etiquetas = etiquetas != null ? etiquetas : new ArrayList<>(); }

    /** Retorna as etiquetas como texto separado por vírgula para exibição. */
    public String getEtiquetasTexto() {
        StringBuilder sb = new StringBuilder();
        for (Etiqueta e : etiquetas) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(e.getNome());
        }
        return sb.toString();
    }
}
