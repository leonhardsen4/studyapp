package com.leonhardsen.studyapp.model;

/**
 * Representa um item na árvore hierárquica de arquivos do sistema.
 * Um item pode ser um caderno (pasta), uma nota Markdown ou um documento PDF.
 * A hierarquia é modelada como lista de adjacência: cada item referencia seu pai via {@code paiId}.
 *
 * @author StudyApp
 * @version 1.0
 */
public class ItemArvore {

    private int id;
    private int usuarioId;
    private Integer paiId;
    private String nome;
    private TipoItem tipo;
    private int posicao;
    private boolean arquivado;

    /**
     * Construtor padrão sem argumentos.
     */
    public ItemArvore() {
    }

    /**
     * Construtor completo para criação de um item com todos os atributos.
     *
     * @param id        identificador único gerado pelo banco de dados
     * @param usuarioId identificador do usuário dono do item
     * @param paiId     identificador do item pai (caderno), ou {@code null} se for raiz
     * @param nome      nome exibido na árvore
     * @param tipo      tipo do item (CADERNO, NOTA ou PDF)
     * @param posicao   ordem de exibição entre itens irmãos
     */
    public ItemArvore(int id, int usuarioId, Integer paiId, String nome, TipoItem tipo, int posicao) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.paiId = paiId;
        this.nome = nome;
        this.tipo = tipo;
        this.posicao = posicao;
    }

    /**
     * Retorna o identificador único do item.
     *
     * @return id do item
     */
    public int getId() {
        return id;
    }

    /**
     * Define o identificador único do item.
     *
     * @param id novo identificador
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Retorna o identificador do usuário dono deste item.
     *
     * @return id do usuário
     */
    public int getUsuarioId() {
        return usuarioId;
    }

    /**
     * Define o identificador do usuário dono deste item.
     *
     * @param usuarioId id do usuário
     */
    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    /**
     * Retorna o identificador do item pai (caderno que contém este item).
     *
     * @return id do pai, ou {@code null} se o item estiver na raiz
     */
    public Integer getPaiId() {
        return paiId;
    }

    /**
     * Define o identificador do item pai.
     *
     * @param paiId id do pai, ou {@code null} para tornar o item raiz
     */
    public void setPaiId(Integer paiId) {
        this.paiId = paiId;
    }

    /**
     * Retorna o nome exibido do item na árvore.
     *
     * @return nome do item
     */
    public String getNome() {
        return nome;
    }

    /**
     * Define o nome exibido do item na árvore.
     *
     * @param nome novo nome
     */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Retorna o tipo do item (CADERNO, NOTA ou PDF).
     *
     * @return tipo do item
     */
    public TipoItem getTipo() {
        return tipo;
    }

    /**
     * Define o tipo do item.
     *
     * @param tipo novo tipo
     */
    public void setTipo(TipoItem tipo) {
        this.tipo = tipo;
    }

    /**
     * Retorna a posição do item entre seus irmãos na árvore.
     *
     * @return posição (usado para ordenação)
     */
    public int getPosicao() {
        return posicao;
    }

    /**
     * Define a posição do item entre seus irmãos na árvore.
     *
     * @param posicao nova posição
     */
    public void setPosicao(int posicao) {
        this.posicao = posicao;
    }

    /**
     * Retorna se o item está arquivado.
     *
     * @return {@code true} se o item estiver arquivado
     */
    public boolean isArquivado() {
        return arquivado;
    }

    /**
     * Define se o item está arquivado.
     *
     * @param arquivado {@code true} para arquivar o item
     */
    public void setArquivado(boolean arquivado) {
        this.arquivado = arquivado;
    }

    @Override
    public String toString() {
        return nome;
    }
}
