package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.Disciplina;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Objeto de acesso a dados (DAO) para a entidade {@link Disciplina}.
 * Realiza operações CRUD na tabela {@code disciplina} do banco de dados SQLite.
 */
public class DisciplinaDAO {

    private final DatabaseManager db;

    /**
     * Cria um novo DAO usando o gerenciador de banco de dados informado.
     *
     * @param db gerenciador de conexão com o banco de dados
     */
    public DisciplinaDAO(DatabaseManager db) { this.db = db; }

    /**
     * Insere uma nova disciplina no banco de dados e retorna o ID gerado.
     *
     * @param d disciplina a ser inserida (sem ID definido)
     * @return ID gerado pelo banco de dados
     * @throws SQLException se ocorrer erro na operação ou a chave gerada não for obtida
     */
    public int inserir(Disciplina d) throws SQLException {
        String sql = "INSERT INTO disciplina (usuario_id, nome) VALUES (?, ?)";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getUsuarioId());
            ps.setString(2, d.getNome().trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Falha ao obter ID da disciplina inserida.");
    }

    /**
     * Atualiza o nome de uma disciplina existente no banco de dados.
     *
     * @param d disciplina com os dados atualizados (deve ter ID válido)
     * @throws SQLException se ocorrer erro na operação
     */
    public void atualizar(Disciplina d) throws SQLException {
        String sql = "UPDATE disciplina SET nome = ? WHERE id = ?";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setString(1, d.getNome().trim());
            ps.setInt(2, d.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Exclui a disciplina com o ID informado (em cascata, exclui também os assuntos vinculados).
     *
     * @param id identificador da disciplina a excluir
     * @throws SQLException se ocorrer erro na operação
     */
    public void excluir(int id) throws SQLException {
        try (PreparedStatement ps = db.getConexao().prepareStatement("DELETE FROM disciplina WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Retorna todas as disciplinas de um usuário, ordenadas pelo nome.
     *
     * @param usuarioId identificador do usuário
     * @return lista de disciplinas (pode estar vazia)
     * @throws SQLException se ocorrer erro na consulta
     */
    public List<Disciplina> buscarPorUsuario(int usuarioId) throws SQLException {
        List<Disciplina> lista = new ArrayList<>();
        String sql = "SELECT id, usuario_id, nome, criado_em FROM disciplina WHERE usuario_id = ? ORDER BY nome COLLATE NOCASE";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /**
     * Conta quantos assuntos estão vinculados a uma disciplina.
     *
     * @param disciplinaId identificador da disciplina
     * @return total de assuntos vinculados
     * @throws SQLException se ocorrer erro na consulta
     */
    public int contarAssuntos(int disciplinaId) throws SQLException {
        try (PreparedStatement ps = db.getConexao().prepareStatement(
                "SELECT COUNT(*) FROM assunto WHERE disciplina_id = ?")) {
            ps.setInt(1, disciplinaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private Disciplina mapear(ResultSet rs) throws SQLException {
        Disciplina d = new Disciplina();
        d.setId(rs.getInt("id"));
        d.setUsuarioId(rs.getInt("usuario_id"));
        d.setNome(rs.getString("nome"));
        String criado = rs.getString("criado_em");
        if (criado != null) d.setCriadoEm(LocalDateTime.parse(criado.replace(" ", "T")));
        return d;
    }
}
