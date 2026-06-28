package com.leonhardsen.studyapp.service;

import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.database.ItemArvoreDAO;
import com.leonhardsen.studyapp.database.NotaDAO;
import com.leonhardsen.studyapp.database.PdfDocumentoDAO;
import com.leonhardsen.studyapp.model.ItemArvore;
import com.leonhardsen.studyapp.model.Nota;
import com.leonhardsen.studyapp.model.PdfDocumento;
import com.leonhardsen.studyapp.model.TipoItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pelas operações de negócio sobre a árvore de arquivos.
 * Gerencia a criação, renomeação, exclusão, movimentação e upload de itens,
 * coordenando os DAOs e o sistema de arquivos local para PDFs e imagens.
 *
 * @author StudyApp
 * @version 1.0
 */
public class ArquivoService {

    private final ItemArvoreDAO itemDAO;
    private final NotaDAO notaDAO;
    private final PdfDocumentoDAO pdfDAO;

    /**
     * Construtor que inicializa os DAOs necessários.
     */
    public ArquivoService() {
        DatabaseManager db = DatabaseManager.getInstance();
        this.itemDAO = new ItemArvoreDAO(db);
        this.notaDAO = new NotaDAO(db);
        this.pdfDAO = new PdfDocumentoDAO(db);
    }

    /**
     * Busca todos os itens da árvore pertencentes a um usuário.
     *
     * @param usuarioId identificador do usuário
     * @return lista completa de itens do usuário
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public List<ItemArvore> buscarTodos(int usuarioId) throws SQLException {
        return itemDAO.buscarTodos(usuarioId);
    }

    /**
     * Busca todos os cadernos de um usuário (independente do nível na hierarquia).
     *
     * @param usuarioId identificador do usuário
     * @return lista de cadernos do usuário
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public List<ItemArvore> buscarCadernos(int usuarioId) throws SQLException {
        return itemDAO.buscarCadernos(usuarioId);
    }

    /**
     * Cria um novo item na árvore. Se for do tipo NOTA, cria também o registro de conteúdo vazio.
     *
     * @param item objeto com os dados do item a ser criado
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void criarItem(ItemArvore item) throws SQLException {
        itemDAO.inserir(item);
        if (item.getTipo() == TipoItem.NOTA) {
            Nota nota = new Nota();
            nota.setItemId(item.getId());
            nota.setConteudo("");
            notaDAO.inserir(nota);
        }
    }

    /**
     * Renomeia um item da árvore.
     *
     * @param itemId   identificador do item
     * @param novoNome novo nome para o item
     * @throws IllegalArgumentException se o nome estiver em branco
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void renomear(int itemId, String novoNome) throws SQLException {
        if (novoNome == null || novoNome.isBlank()) {
            throw new IllegalArgumentException("O nome não pode estar em branco.");
        }
        itemDAO.atualizarNome(itemId, novoNome.trim());
    }

    /**
     * Exclui um item da árvore. Para cadernos, remove todos os filhos em cascata.
     * Para PDFs, remove também o arquivo físico do disco.
     *
     * @param item objeto do item a ser excluído
     * @throws SQLException se ocorrer erro no banco de dados
     * @throws IOException  se ocorrer erro ao excluir arquivo PDF do disco
     */
    public void excluir(ItemArvore item) throws SQLException, IOException {
        if (item.getTipo() == TipoItem.PDF) {
            PdfDocumento pdf = pdfDAO.buscarPorItemId(item.getId());
            if (pdf != null) {
                new File(pdf.getCaminhoArquivo()).delete();
            }
        }
        itemDAO.excluir(item.getId());
    }

    /**
     * Move um item para um novo caderno pai.
     *
     * @param itemId    identificador do item a ser movido
     * @param novoPaiId identificador do novo caderno pai, ou {@code null} para raiz
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void mover(int itemId, Integer novoPaiId) throws SQLException {
        itemDAO.mover(itemId, novoPaiId);
    }

    /**
     * Conta os descendentes de um caderno para exibir ao usuário antes de confirmação de exclusão.
     *
     * @param itemId    identificador do caderno
     * @param usuarioId identificador do usuário
     * @return número total de itens que serão removidos junto com o caderno
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public int contarDescendentes(int itemId, int usuarioId) throws SQLException {
        return itemDAO.contarDescendentes(itemId, usuarioId);
    }

    /**
     * Realiza o upload de um arquivo PDF: copia o arquivo para o diretório de dados da aplicação
     * e registra os metadados no banco de dados vinculados ao item da árvore.
     *
     * @param item         item da árvore do tipo PDF (já deve estar inserido no banco)
     * @param arquivoOrigem arquivo PDF selecionado pelo usuário
     * @throws SQLException se ocorrer erro no banco de dados
     * @throws IOException  se ocorrer erro ao copiar o arquivo
     */
    public void uploadPdf(ItemArvore item, File arquivoOrigem) throws SQLException, IOException {
        String dirPdfs = DatabaseManager.getDirApp() + "/pdfs/" + item.getUsuarioId();
        new File(dirPdfs).mkdirs();

        String extensao = ".pdf";
        String nomeArquivo = UUID.randomUUID().toString() + extensao;
        File destino = new File(dirPdfs + "/" + nomeArquivo);

        Files.copy(arquivoOrigem.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);

        PdfDocumento pdf = new PdfDocumento();
        pdf.setItemId(item.getId());
        pdf.setCaminhoArquivo(destino.getAbsolutePath());
        pdf.setTamanhoBytes(destino.length());
        pdfDAO.inserir(pdf);
    }

    /**
     * Remove todos os itens de um usuário do banco de dados.
     * Usado exclusivamente pelo serviço de exclusão de conta antes de apagar o registro do usuário.
     *
     * @param usuarioId identificador do usuário
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void excluirTodosItens(int usuarioId) throws SQLException {
        itemDAO.excluirPorUsuario(usuarioId);
    }

    /**
     * Move o item uma posição acima entre seus irmãos (mesmo pai e usuário).
     * Se já for o primeiro, não faz nada.
     *
     * @param itemId    identificador do item
     * @param usuarioId identificador do usuário
     * @param paiId     id do pai, ou {@code null} para itens raiz
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void subirPosicao(int itemId, int usuarioId, Integer paiId) throws SQLException {
        List<ItemArvore> irmaos = itemDAO.buscarFilhos(usuarioId, paiId);
        for (int i = 1; i < irmaos.size(); i++) {
            if (irmaos.get(i).getId() == itemId) {
                Collections.swap(irmaos, i, i - 1);
                for (int j = 0; j < irmaos.size(); j++) {
                    itemDAO.atualizarPosicao(irmaos.get(j).getId(), j);
                }
                return;
            }
        }
    }

    /**
     * Move o item uma posição abaixo entre seus irmãos (mesmo pai e usuário).
     * Se já for o último, não faz nada.
     *
     * @param itemId    identificador do item
     * @param usuarioId identificador do usuário
     * @param paiId     id do pai, ou {@code null} para itens raiz
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void descerPosicao(int itemId, int usuarioId, Integer paiId) throws SQLException {
        List<ItemArvore> irmaos = itemDAO.buscarFilhos(usuarioId, paiId);
        for (int i = 0; i < irmaos.size() - 1; i++) {
            if (irmaos.get(i).getId() == itemId) {
                Collections.swap(irmaos, i, i + 1);
                for (int j = 0; j < irmaos.size(); j++) {
                    itemDAO.atualizarPosicao(irmaos.get(j).getId(), j);
                }
                return;
            }
        }
    }

    /**
     * Busca os metadados de um PDF pelo identificador do item da árvore.
     *
     * @param itemId identificador do item da árvore
     * @return objeto {@link PdfDocumento} com o caminho e metadados, ou {@code null} se não encontrado
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public PdfDocumento buscarPdf(int itemId) throws SQLException {
        return pdfDAO.buscarPorItemId(itemId);
    }

    /**
     * Arquiva um caderno (e seu conteúdo) ocultando-o da árvore de arquivos.
     *
     * @param itemId identificador do caderno a ser arquivado
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void arquivar(int itemId) throws SQLException {
        itemDAO.arquivar(itemId);
    }

    /**
     * Desarquiva um caderno, tornando-o visível novamente na árvore de arquivos.
     *
     * @param itemId identificador do caderno a ser desarquivado
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void desarquivar(int itemId) throws SQLException {
        itemDAO.desarquivar(itemId);
    }

    /**
     * Busca itens da árvore cujo nome ou conteúdo de nota corresponda ao texto informado.
     * Itens arquivados são excluídos dos resultados.
     *
     * @param usuarioId identificador do usuário
     * @param texto     texto de busca (parcial, sem distinção de maiúsculas)
     * @return lista de itens correspondentes ordenados por nome
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public List<ItemArvore> buscarPorTexto(int usuarioId, String texto) throws SQLException {
        return itemDAO.buscarPorTexto(usuarioId, texto);
    }
}
