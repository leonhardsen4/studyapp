package com.leonhardsen.studyapp.service;

import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.database.NotaDAO;
import com.leonhardsen.studyapp.model.Nota;

import java.sql.SQLException;

/**
 * Serviço responsável pelas operações de negócio sobre o conteúdo das notas Markdown.
 * Coordena o carregamento e salvamento de conteúdo, incluindo atualização do timestamp.
 *
 * @author StudyApp
 * @version 1.0
 */
public class NotaService {

    private final NotaDAO notaDAO;

    /**
     * Construtor que inicializa o DAO de notas.
     */
    public NotaService() {
        this.notaDAO = new NotaDAO(DatabaseManager.getInstance());
    }

    /**
     * Carrega o conteúdo completo de uma nota pelo identificador do item da árvore.
     *
     * @param itemId identificador do item da árvore associado à nota
     * @return objeto {@link Nota} com o conteúdo, ou uma nota vazia se não encontrada
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public Nota carregar(int itemId) throws SQLException {
        Nota nota = notaDAO.buscarPorItemId(itemId);
        if (nota == null) {
            nota = new Nota(0, itemId, "");
        }
        return nota;
    }

    /**
     * Salva o conteúdo Markdown de uma nota.
     * Atualiza o campo {@code atualizado_em} do item correspondente na árvore.
     *
     * @param itemId   identificador do item da árvore
     * @param conteudo novo conteúdo Markdown da nota
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void salvar(int itemId, String conteudo) throws SQLException {
        notaDAO.atualizarConteudo(itemId, conteudo);
    }
}
