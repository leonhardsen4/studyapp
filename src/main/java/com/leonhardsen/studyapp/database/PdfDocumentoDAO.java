package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.PdfDocumento;

import java.sql.*;

/**
 * Objeto de acesso a dados para a entidade {@link PdfDocumento}.
 * Gerencia a persistência dos metadados dos documentos PDF no banco de dados.
 *
 * @author StudyApp
 * @version 1.0
 */
public class PdfDocumentoDAO {

    private final DatabaseManager dbManager;

    /**
     * Construtor que recebe a instância do gerenciador de banco de dados.
     *
     * @param dbManager gerenciador de conexão com o banco de dados
     */
    public PdfDocumentoDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Insere um novo documento PDF no banco de dados.
     * O campo {@code id} do objeto é atualizado com o valor gerado automaticamente.
     *
     * @param pdf objeto com os dados do PDF a ser inserido
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void inserir(PdfDocumento pdf) throws SQLException {
        String sql = "INSERT INTO pdf_documento (item_id, caminho_arquivo, tamanho_bytes) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, pdf.getItemId());
            stmt.setString(2, pdf.getCaminhoArquivo());
            stmt.setLong(3, pdf.getTamanhoBytes());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    pdf.setId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Busca os metadados de um PDF pelo identificador do item da árvore.
     *
     * @param itemId identificador do item da árvore correspondente ao PDF
     * @return objeto {@link PdfDocumento} com os metadados, ou {@code null} se não encontrado
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public PdfDocumento buscarPorItemId(int itemId) throws SQLException {
        String sql = "SELECT * FROM pdf_documento WHERE item_id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PdfDocumento(
                            rs.getInt("id"),
                            rs.getInt("item_id"),
                            rs.getString("caminho_arquivo"),
                            rs.getLong("tamanho_bytes")
                    );
                }
            }
        }
        return null;
    }
}
