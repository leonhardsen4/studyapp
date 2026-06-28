package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.SessaoPomodoro;
import com.leonhardsen.studyapp.model.TipoSessao;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
     * Soma a duração total (em segundos) de todas as sessões de foco realizadas para um assunto.
     *
     * @param assuntoId identificador do assunto
     * @return total de segundos de foco acumulados no assunto, ou {@code 0} se nenhuma sessão existir
     * @throws SQLException se ocorrer erro na consulta
     */
    public int somarDuracaoPorAssunto(int assuntoId) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(duracao_segundos), 0) FROM sessao_pomodoro
            WHERE assunto_id = ? AND tipo = 'FOCO'
            """;
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, assuntoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Soma a duração total (em segundos) de todas as sessões de foco realizadas para todos os
     * assuntos de uma disciplina.
     *
     * @param disciplinaId identificador da disciplina
     * @return total de segundos de foco acumulados na disciplina, ou {@code 0} se nenhuma sessão existir
     * @throws SQLException se ocorrer erro na consulta
     */
    public int somarDuracaoPorDisciplina(int disciplinaId) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(sp.duracao_segundos), 0)
            FROM sessao_pomodoro sp
            JOIN assunto a ON sp.assunto_id = a.id
            WHERE a.disciplina_id = ? AND sp.tipo = 'FOCO'
            """;
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, disciplinaId);
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

    /**
     * Retorna um resumo das últimas sessões de foco do usuário, com o nome do assunto e
     * da disciplina vinculados (via JOIN). Cada elemento do resultado é um array de strings
     * com os campos: {@code [duracao_segundos, concluido_em, assunto_nome, disciplina_nome]},
     * onde {@code assunto_nome} e {@code disciplina_nome} podem ser {@code null}.
     *
     * @param usuarioId identificador do usuário
     * @param limite    número máximo de sessões a retornar
     * @return lista de arrays de strings, ordenada da mais recente para a mais antiga
     * @throws SQLException se ocorrer erro na consulta
     */
    public List<String[]> buscarResumoRecentes(int usuarioId, int limite) throws SQLException {
        String sql = """
            SELECT sp.duracao_segundos, sp.concluido_em,
                   a.nome  AS assunto_nome,
                   d.nome  AS disciplina_nome
            FROM sessao_pomodoro sp
            LEFT JOIN assunto    a ON sp.assunto_id    = a.id
            LEFT JOIN disciplina d ON a.disciplina_id  = d.id
            WHERE sp.usuario_id = ? AND sp.tipo = 'FOCO'
            ORDER BY sp.concluido_em DESC
            LIMIT ?
            """;
        List<String[]> lista = new ArrayList<>();
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setInt(2, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new String[]{
                        String.valueOf(rs.getInt("duracao_segundos")),
                        rs.getString("concluido_em"),
                        rs.getString("assunto_nome"),
                        rs.getString("disciplina_nome")
                    });
                }
            }
        }
        return lista;
    }
}
