package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.Assunto;
import com.leonhardsen.studyapp.model.TipoDificuldade;
import com.leonhardsen.studyapp.model.TipoStatusAssunto;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Objeto de acesso a dados (DAO) para a entidade {@link Assunto}.
 * Realiza operações CRUD na tabela {@code assunto} do banco de dados SQLite.
 */
public class AssuntoDAO {

    private final DatabaseManager db;

    /**
     * Cria um novo DAO usando o gerenciador de banco de dados informado.
     *
     * @param db gerenciador de conexão com o banco de dados
     */
    public AssuntoDAO(DatabaseManager db) { this.db = db; }

    /**
     * Insere um novo assunto no banco de dados e retorna o ID gerado.
     *
     * @param a assunto a ser inserido (sem ID definido)
     * @return ID gerado pelo banco de dados
     * @throws SQLException se ocorrer erro na operação ou a chave gerada não for obtida
     */
    public int inserir(Assunto a) throws SQLException {
        String sql = """
            INSERT INTO assunto (disciplina_id, nome, dificuldade, sessoes_minimas,
                                 sessoes_realizadas, status, data_limite)
            VALUES (?, ?, ?, ?, 0, 'PENDENTE', ?)
            """;
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getDisciplinaId());
            ps.setString(2, a.getNome().trim());
            ps.setString(3, a.getDificuldade().name());
            ps.setInt(4, a.getSessoesMinimas());
            ps.setString(5, a.getDataLimite() != null ? a.getDataLimite().toString() : null);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Falha ao obter ID do assunto inserido.");
    }

    /**
     * Atualiza os dados de um assunto existente no banco de dados.
     *
     * @param a assunto com os dados atualizados (deve ter ID válido)
     * @throws SQLException se ocorrer erro na operação
     */
    public void atualizar(Assunto a) throws SQLException {
        String sql = """
            UPDATE assunto SET nome = ?, dificuldade = ?, sessoes_minimas = ?,
                               sessoes_realizadas = ?, status = ?, data_limite = ?,
                               atualizado_em = datetime('now')
            WHERE id = ?
            """;
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setString(1, a.getNome().trim());
            ps.setString(2, a.getDificuldade().name());
            ps.setInt(3, a.getSessoesMinimas());
            ps.setInt(4, a.getSessoesRealizadas());
            ps.setString(5, a.getStatus().name());
            ps.setString(6, a.getDataLimite() != null ? a.getDataLimite().toString() : null);
            ps.setInt(7, a.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Exclui o assunto com o ID informado.
     *
     * @param id identificador do assunto a excluir
     * @throws SQLException se ocorrer erro na operação
     */
    public void excluir(int id) throws SQLException {
        try (PreparedStatement ps = db.getConexao().prepareStatement("DELETE FROM assunto WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Busca um assunto pelo seu identificador único.
     *
     * @param id identificador do assunto
     * @return o assunto encontrado, ou {@code null} se não existir
     * @throws SQLException se ocorrer erro na consulta
     */
    public Assunto buscarPorId(int id) throws SQLException {
        try (PreparedStatement ps = db.getConexao().prepareStatement(
                "SELECT * FROM assunto WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Retorna todos os assuntos vinculados a uma disciplina, ordenados pelo nome.
     *
     * @param disciplinaId identificador da disciplina
     * @return lista de assuntos (pode estar vazia)
     * @throws SQLException se ocorrer erro na consulta
     */
    public List<Assunto> buscarPorDisciplina(int disciplinaId) throws SQLException {
        List<Assunto> lista = new ArrayList<>();
        String sql = "SELECT * FROM assunto WHERE disciplina_id = ? ORDER BY nome COLLATE NOCASE";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, disciplinaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private Assunto mapear(ResultSet rs) throws SQLException {
        Assunto a = new Assunto();
        a.setId(rs.getInt("id"));
        a.setDisciplinaId(rs.getInt("disciplina_id"));
        a.setNome(rs.getString("nome"));
        a.setDificuldade(TipoDificuldade.valueOf(rs.getString("dificuldade")));
        a.setSessoesMinimas(rs.getInt("sessoes_minimas"));
        a.setSessoesRealizadas(rs.getInt("sessoes_realizadas"));
        a.setStatus(TipoStatusAssunto.valueOf(rs.getString("status")));
        String limite = rs.getString("data_limite");
        if (limite != null) a.setDataLimite(LocalDate.parse(limite));
        String criado = rs.getString("criado_em");
        if (criado != null) a.setCriadoEm(LocalDateTime.parse(criado.replace(" ", "T")));
        String atualizado = rs.getString("atualizado_em");
        if (atualizado != null) a.setAtualizadoEm(LocalDateTime.parse(atualizado.replace(" ", "T")));
        return a;
    }
}
