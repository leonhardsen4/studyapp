package com.leonhardsen.studyapp.model;

/**
 * Representa uma etiqueta (tag) criada por um usuário.
 * Etiquetas são usadas para categorizar e filtrar {@link Tarefa}s.
 * Cada tarefa pode ter no máximo 3 etiquetas.
 */
public class Etiqueta {
    private int id;
    private int usuarioId;
    private String nome;
    private String criadoEm;

    /** Construtor padrão sem argumentos. */
    public Etiqueta() {}

    /**
     * Construtor completo para reconstrução a partir do banco de dados.
     *
     * @param id        identificador único da etiqueta
     * @param usuarioId identificador do usuário proprietário
     * @param nome      nome da etiqueta
     * @param criadoEm  data e hora de criação no formato ISO (texto)
     */
    public Etiqueta(int id, int usuarioId, String nome, String criadoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.criadoEm = criadoEm;
    }

    /** Retorna o identificador único da etiqueta. */
    public int getId() { return id; }
    /** Define o identificador único da etiqueta. */
    public void setId(int id) { this.id = id; }
    /** Retorna o identificador do usuário proprietário. */
    public int getUsuarioId() { return usuarioId; }
    /** Define o identificador do usuário proprietário. */
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    /** Retorna o nome da etiqueta. */
    public String getNome() { return nome; }
    /** Define o nome da etiqueta. */
    public void setNome(String nome) { this.nome = nome; }
    /** Retorna a data/hora de criação como texto. */
    public String getCriadoEm() { return criadoEm; }
    /** Define a data/hora de criação como texto. */
    public void setCriadoEm(String criadoEm) { this.criadoEm = criadoEm; }

    @Override
    public String toString() { return nome; }

    /**
     * Compara etiquetas pelo identificador único.
     *
     * @param obj objeto a comparar
     * @return {@code true} se os IDs forem iguais
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Etiqueta other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() { return Integer.hashCode(id); }
}
