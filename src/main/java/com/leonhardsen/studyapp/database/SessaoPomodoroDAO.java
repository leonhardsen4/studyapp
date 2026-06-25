package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.SessaoPomodoro;
import com.leonhardsen.studyapp.model.TipoSessao;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Objeto de acesso a dados (DAO) para a entidade {@link SessaoPomodoro}.
 * Realiza operações de registro e consulta na tabela {@code sessao_pomodoro} do banco de dados SQLite.
 */
public class SessaoPomodoroDAO {

    private final DatabaseManager db;

    /**
     * Cria um novo DAO usando o gerenciador de banco de dados informado.
     *
     * @param db gerenciador de conexão com o banco de dados
     */
    public SessaoPomodoroDAO(DatabaseManager db) { this.db = db; }

    /**
     * Registra uma sessão Pomodoro concluída no banco de dados.
     *
     * @param s sessão a ser registrada
     * @throws SQLException se ocorrer erro na operação
     */
    public void registrar(SessaoPomodoro s) throws SQLException {
        String sql = """
            INSERT INTO sessao_pomodoro (usuario_id, assunto_id, tipo, iniciado_em, concluido_em, duracao_segundos)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, s.getUsuarioId());
            if (s.getAssuntoId() != null) ps.setInt(2, s.getAssuntoId());
            else ps.setNull(2, Types.INTEGER);
            ps.setString(3, s.getTipo().name());
            ps.setString(4, s.getIniciadoEm().toString());
            ps.setString(5, s.getConcluidoEm().toString());
            ps.setInt(6, s.getDuracaoSegundos());
            ps.executeUpdate();
        }
    }

    /**
     * Conta o número de sessões de foco concluídas pelo usuário no dia atual.
     *
     * @param usuarioId identificador do usuário
     * @return quantidade de sessões de foco concluídas hoje
     * @throws SQLException se ocorrer erro na consulta
     */
    public int contarSessoesHoje(int usuarioId) throws SQLException {
        String hoje = LocalDate.now().toString();
        String sql = """
            SELECT COUNT(*) FROM sessao_pomodoro
            WHERE usuario_id = ? AND tipo = 'FOCO' AND date(concluido_em) = ?
            """;
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, hoje);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Soma a duração total (em segundos) de todas as sessões de foco concluídas hoje pelo usuário.
     *
     * @param usuarioId identificador do usuário
     * @return total de segundos de foco acumulados hoje, ou {@code 0} se nenhuma sessão existir
     * @throws SQLException se ocorrer erro na consulta
     */
    public int somarDuracaoHoje(int usuarioId) throws SQLException {
        String hoje = LocalDate.now().toString();
        String sql = """
            SELECT COALESCE(SUM(duracao_segundos), 0) FROM sessao_pomodoro
            WHERE usuario_id = ? AND tipo = 'FOCO' AND date(concluido_em) = ?
            """;
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, hoje);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
