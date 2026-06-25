package com.leonhardsen.studyapp.model;

/**
 * Representa a referência a um arquivo PDF associado a um {@link ItemArvore} do tipo PDF.
 * O arquivo físico é armazenado no sistema de arquivos local e o banco guarda apenas o caminho.
 *
 * @author StudyApp
 * @version 1.0
 */
public class PdfDocumento {

    private int id;
    private int itemId;
    private String caminhoArquivo;
    private long tamanhoBytes;

    /**
     * Construtor padrão sem argumentos.
     */
    public PdfDocumento() {
    }

    /**
     * Construtor completo para criação de um documento PDF com todos os atributos.
     *
     * @param id             identificador único gerado pelo banco de dados
     * @param itemId         identificador do {@link ItemArvore} ao qual este PDF pertence
     * @param caminhoArquivo caminho absoluto do arquivo PDF no disco local
     * @param tamanhoBytes   tamanho do arquivo em bytes
     */
    public PdfDocumento(int id, int itemId, String caminhoArquivo, long tamanhoBytes) {
        this.id = id;
        this.itemId = itemId;
        this.caminhoArquivo = caminhoArquivo;
        this.tamanhoBytes = tamanhoBytes;
    }

    /**
     * Retorna o identificador único do documento PDF.
     *
     * @return id do documento
     */
    public int getId() {
        return id;
    }

    /**
     * Define o identificador único do documento PDF.
     *
     * @param id novo identificador
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Retorna o identificador do item da árvore ao qual este PDF pertence.
     *
     * @return id do item
     */
    public int getItemId() {
        return itemId;
    }

    /**
     * Define o identificador do item da árvore ao qual este PDF pertence.
     *
     * @param itemId id do item
     */
    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    /**
     * Retorna o caminho absoluto do arquivo PDF no disco local.
     *
     * @return caminho do arquivo
     */
    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }

    /**
     * Define o caminho absoluto do arquivo PDF no disco local.
     *
     * @param caminhoArquivo novo caminho do arquivo
     */
    public void setCaminhoArquivo(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
    }

    /**
     * Retorna o tamanho do arquivo PDF em bytes.
     *
     * @return tamanho em bytes
     */
    public long getTamanhoBytes() {
        return tamanhoBytes;
    }

    /**
     * Define o tamanho do arquivo PDF em bytes.
     *
     * @param tamanhoBytes tamanho em bytes
     */
    public void setTamanhoBytes(long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }
}
