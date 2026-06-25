package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.Etiqueta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Objeto de acesso a dados (DAO) para a entidade {@link Etiqueta}.
 * Realiza operações CRUD na tabela {@code etiqueta} do banco de dados SQLite,
 * incluindo gerenciamento do vínculo com tarefas via {@code tarefa_etiqueta}.
 */
public class EtiquetaDAO {

    private final DatabaseManager db;

    /**
     * Cria um novo DAO usando o gerenciador de banco de dados informado.
     *
     * @param db gerenciador de conexão com o banco de dados
     */
    public EtiquetaDAO(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Insere uma nova etiqueta no banco de dados e retorna o ID gerado.
     *
     * @param usuarioId identificador do usuário proprietário
     * @param nome      nome da etiqueta
     * @return ID gerado pelo banco de dados, ou {@code -1} em caso de falha
     * @throws SQLException se ocorrer erro na operação
     */
    public int inserir(int usuarioId, String nome) throws SQLException {
        String sql = "INSERT INTO etiqueta(usuario_id, nome) VALUES(?, ?)";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, nome.trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    /**
     * Retorna todas as etiquetas de um usuário, ordenadas pelo nome.
     *
     * @param usuarioId identificador do usuário
     * @return lista de etiquetas (pode estar vazia)
     * @throws SQLException se ocorrer erro na consulta
     */
    public List<Etiqueta> buscarTodas(int usuarioId) throws SQLException {
        String sql = "SELECT id, usuario_id, nome, criado_em FROM etiqueta WHERE usuario_id = ? ORDER BY nome COLLATE NOCASE";
        List<Etiqueta> lista = new ArrayList<>();
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Busca uma etiqueta pelo seu identificador único.
     *
     * @param id identificador da etiqueta
     * @return a etiqueta encontrada, ou {@code null} se não existir
     * @throws SQLException se ocorrer erro na consulta
     */
    public Etiqueta buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, usuario_id, nome, criado_em FROM etiqueta WHERE id = ?";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Atualiza o nome de uma etiqueta existente no banco de dados.
     *
     * @param id       identificador da etiqueta
     * @param novoNome novo nome a ser definido
     * @throws SQLException se ocorrer erro na operação
     */
    public void atualizarNome(int id, String novoNome) throws SQLException {
        String sql = "UPDATE etiqueta SET nome = ? WHERE id = ?";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setString(1, novoNome.trim());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Exclui a etiqueta com o ID informado e remove todos os vínculos com tarefas.
     *
     * @param id identificador da etiqueta a excluir
     * @throws SQLException se ocorrer erro na operação
     */
    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM etiqueta WHERE id = ?";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Retorna os IDs das tarefas que possuem SOMENTE esta etiqueta (sem outras). */
    public List<Integer> buscarIdsTarefasExclusivas(int etiquetaId) throws SQLException {
        String sql = """
            SELECT t.id FROM tarefa t
            WHERE t.id IN (SELECT tarefa_id FROM tarefa_etiqueta WHERE etiqueta_id = ?)
            AND (SELECT COUNT(*) FROM tarefa_etiqueta te WHERE te.tarefa_id = t.id) = 1
            """;
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, etiquetaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    private Etiqueta mapear(ResultSet rs) throws SQLException {
        return new Etiqueta(
            rs.getInt("id"),
            rs.getInt("usuario_id"),
            rs.getString("nome"),
            rs.getString("criado_em")
        );
    }
}
