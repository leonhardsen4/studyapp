package com.leonhardsen.studyapp.model;

/**
 * Representa um usuário cadastrado no sistema.
 * Contém as informações de autenticação e preferências do usuário.
 *
 * @author StudyApp
 * @version 1.0
 */
public class Usuario {

    private int id;
    private String nome;
    private String email;
    private String senhaHash;
    private String tema;

    /**
     * Construtor padrão sem argumentos.
     */
    public Usuario() {
    }

    /**
     * Construtor completo para criação de um usuário com todos os atributos.
     *
     * @param id       identificador único gerado pelo banco de dados
     * @param nome     nome completo do usuário
     * @param email    endereço de e-mail único do usuário
     * @param senhaHash hash BCrypt da senha do usuário
     * @param tema     preferência de tema visual: "CLARO" ou "ESCURO"
     */
    public Usuario(int id, String nome, String email, String senhaHash, String tema) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.tema = tema;
    }

    /**
     * Retorna o identificador único do usuário.
     *
     * @return id do usuário
     */
    public int getId() {
        return id;
    }

    /**
     * Define o identificador único do usuário.
     *
     * @param id novo identificador
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Retorna o nome completo do usuário.
     *
     * @return nome do usuário
     */
    public String getNome() {
        return nome;
    }

    /**
     * Define o nome completo do usuário.
     *
     * @param nome novo nome
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Retorna o endereço de e-mail do usuário.
     *
     * @return e-mail do usuário
     */
    public String getEmail() {
        return email;
    }

    /**
     * Define o endereço de e-mail do usuário.
     *
     * @param email novo e-mail
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Retorna o hash BCrypt da senha do usuário.
     *
     * @return hash da senha
     */
    public String getSenhaHash() {
        return senhaHash;
    }

    /**
     * Define o hash BCrypt da senha do usuário.
     *
     * @param senhaHash novo hash de senha
     */
    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    /**
     * Retorna a preferência de tema visual do usuário.
     *
     * @return "CLARO" ou "ESCURO"
     */
    public String getTema() {
        return tema;
    }

    /**
     * Define a preferência de tema visual do usuário.
     *
     * @param tema novo tema ("CLARO" ou "ESCURO")
     */
    public void setTema(String tema) {
        this.tema = tema;
    }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", nome='" + nome + "', email='" + email + "'}";
    }
}
