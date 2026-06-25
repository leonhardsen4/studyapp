package com.leonhardsen.studyapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public Tarefa() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getAnotacoes() { return anotacoes != null ? anotacoes : ""; }
    public void setAnotacoes(String anotacoes) { this.anotacoes = anotacoes; }
    public TipoPrioridade getPrioridade() { return prioridade; }
    public void setPrioridade(TipoPrioridade prioridade) { this.prioridade = prioridade; }
    public TipoStatus getStatus() { return status; }
    public void setStatus(TipoStatus status) { this.status = status; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public List<Etiqueta> getEtiquetas() { return etiquetas; }
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
