package com.leonhardsen.studyapp.model;

/**
 * Representa o conteúdo Markdown de uma nota associada a um {@link ItemArvore} do tipo NOTA.
 * O conteúdo é armazenado separadamente do item para evitar carregamento desnecessário ao montar a árvore.
 *
 * @author StudyApp
 * @version 1.0
 */
public class Nota {

    private int id;
    private int itemId;
    private String conteudo;

    /**
     * Construtor padrão sem argumentos.
     */
    public Nota() {
    }

    /**
     * Construtor completo para criação de uma nota com todos os atributos.
     *
     * @param id       identificador único gerado pelo banco de dados
     * @param itemId   identificador do {@link ItemArvore} ao qual esta nota pertence
     * @param conteudo texto em formato Markdown da nota
     */
    public Nota(int id, int itemId, String conteudo) {
        this.id = id;
        this.itemId = itemId;
        this.conteudo = conteudo;
    }

    /**
     * Retorna o identificador único da nota.
     *
     * @return id da nota
     */
    public int getId() {
        return id;
    }

    /**
     * Define o identificador único da nota.
     *
     * @param id novo identificador
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Retorna o identificador do item da árvore ao qual esta nota pertence.
     *
     * @return id do item
     */
    public int getItemId() {
        return itemId;
    }

    /**
     * Define o identificador do item da árvore ao qual esta nota pertence.
     *
     * @param itemId id do item
     */
    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    /**
     * Retorna o conteúdo Markdown da nota.
     *
     * @return texto em Markdown
     */
    public String getConteudo() {
        return conteudo;
    }

    /**
     * Define o conteúdo Markdown da nota.
     *
     * @param conteudo novo texto em Markdown
     */
    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }
}
