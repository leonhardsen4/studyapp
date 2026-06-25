package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.Nota;

import java.sql.*;

/**
 * Objeto de acesso a dados para a entidade {@link Nota}.
 * Gerencia a persistência do conteúdo Markdown das notas no banco de dados.
 *
 * @author StudyApp
 * @version 1.0
 */
public class NotaDAO {

    private final DatabaseManager dbManager;

    /**
     * Construtor que recebe a instância do gerenciador de banco de dados.
     *
     * @param dbManager gerenciador de conexão com o banco de dados
     */
    public NotaDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Insere uma nova nota no banco de dados.
     * O campo {@code id} do objeto é atualizado com o valor gerado automaticamente.
     *
     * @param nota objeto com os dados da nota a ser inserida
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void inserir(Nota nota) throws SQLException {
        String sql = "INSERT INTO nota (item_id, conteudo) VALUES (?, ?)";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, nota.getItemId());
            stmt.setString(2, nota.getConteudo() != null ? nota.getConteudo() : "");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    nota.setId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Busca o conteúdo de uma nota pelo identificador do item da árvore.
     *
     * @param itemId identificador do item da árvore correspondente à nota
     * @return objeto {@link Nota} com o conteúdo, ou {@code null} se não encontrado
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public Nota buscarPorItemId(int itemId) throws SQLException {
        String sql = "SELECT * FROM nota WHERE item_id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Nota(rs.getInt("id"), rs.getInt("item_id"), rs.getString("conteudo"));
                }
            }
        }
        return null;
    }

    /**
     * Atualiza o conteúdo Markdown de uma nota existente.
     * Também atualiza o campo {@code atualizado_em} do item correspondente na árvore.
     *
     * @param itemId   identificador do item da árvore correspondente à nota
     * @param conteudo novo conteúdo Markdown
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void atualizarConteudo(int itemId, String conteudo) throws SQLException {
        String sqlNota = "UPDATE nota SET conteudo = ? WHERE item_id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sqlNota)) {
            stmt.setString(1, conteudo);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
        }
        String sqlItem = "UPDATE item_arvore SET atualizado_em = datetime('now') WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sqlItem)) {
            stmt.setInt(1, itemId);
            stmt.executeUpdate();
        }
    }
}
