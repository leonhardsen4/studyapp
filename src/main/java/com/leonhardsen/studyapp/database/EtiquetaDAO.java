package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.Etiqueta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EtiquetaDAO {

    private final DatabaseManager db;

    public EtiquetaDAO(DatabaseManager db) {
        this.db = db;
    }

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

    public Etiqueta buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, usuario_id, nome, criado_em FROM etiqueta WHERE id = ?";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void atualizarNome(int id, String novoNome) throws SQLException {
        String sql = "UPDATE etiqueta SET nome = ? WHERE id = ?";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setString(1, novoNome.trim());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

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
