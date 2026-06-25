package com.leonhardsen.studyapp.model;

public class Etiqueta {
    private int id;
    private int usuarioId;
    private String nome;
    private String criadoEm;

    public Etiqueta() {}

    public Etiqueta(int id, int usuarioId, String nome, String criadoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.criadoEm = criadoEm;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCriadoEm() { return criadoEm; }
    public void setCriadoEm(String criadoEm) { this.criadoEm = criadoEm; }

    @Override
    public String toString() { return nome; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Etiqueta other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
