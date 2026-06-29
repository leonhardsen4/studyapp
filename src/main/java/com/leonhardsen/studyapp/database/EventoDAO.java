package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.Evento;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Objeto de acesso a dados (DAO) para a entidade {@link Evento}.
 * Realiza operações CRUD na tabela {@code evento} do banco de dados SQLite.
 */
public class EventoDAO {

    private final DatabaseManager dbManager;
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_DT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Cria um novo DAO usando o gerenciador de banco de dados informado.
     *
     * @param dbManager gerenciador de conexão com o banco de dados
     */
    public EventoDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Insere um novo evento no banco de dados e retorna o ID gerado.
     *
     * @param e evento a ser inserido (sem ID definido)
     * @return ID gerado pelo banco de dados, ou {@code -1} em caso de falha
     * @throws SQLException se ocorrer erro na operação
     */
    public int inserir(Evento e) throws SQLException {
        String sql = """
            INSERT INTO evento (usuario_id, titulo, descricao, data, hora_inicio, hora_fim)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = dbManager.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getUsuarioId());
            ps.setString(2, e.getTitulo());
            ps.setString(3, e.getDescricao());
            ps.setString(4, e.getData().toString());
            ps.setString(5, e.getHoraInicio() != null ? e.getHoraInicio().format(FMT_HORA) : null);
            ps.setString(6, e.getHoraFim()    != null ? e.getHoraFim().format(FMT_HORA)    : null);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    /**
     * Atualiza os dados de um evento existente no banco de dados.
     *
     * @param e evento com os dados atualizados (deve ter ID válido)
     * @throws SQLException se ocorrer erro na operação
     */
    public void atualizar(Evento e) throws SQLException {
        String sql = "UPDATE evento SET titulo=?, descricao=?, data=?, hora_inicio=?, hora_fim=? WHERE id=?";
        try (PreparedStatement ps = dbManager.getConexao().prepareStatement(sql)) {
            ps.setString(1, e.getTitulo());
            ps.setString(2, e.getDescricao());
            ps.setString(3, e.getData().toString());
            ps.setString(4, e.getHoraInicio() != null ? e.getHoraInicio().format(FMT_HORA) : null);
            ps.setString(5, e.getHoraFim()    != null ? e.getHoraFim().format(FMT_HORA)    : null);
            ps.setInt(6, e.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Exclui o evento com o ID informado.
     *
     * @param id identificador do evento a excluir
     * @throws SQLException se ocorrer erro na operação
     */
    public void excluir(int id) throws SQLException {
        try (PreparedStatement ps = dbManager.getConexao().prepareStatement("DELETE FROM evento WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Retorna todos os eventos de um usuário em um mês/ano específico, ordenados por data e hora.
     *
     * @param usuarioId identificador do usuário
     * @param ano       ano desejado
     * @param mes       mês desejado (1–12)
     * @return lista de eventos do período (pode estar vazia)
     * @throws SQLException se ocorrer erro na consulta
     */
    public List<Evento> buscarPorMes(int usuarioId, int ano, int mes) throws SQLException {
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim    = YearMonth.of(ano, mes).atEndOfMonth();
        String sql = """
            SELECT * FROM evento
            WHERE usuario_id = ? AND data >= ? AND data <= ?
            ORDER BY data, hora_inicio
            """;
        try (PreparedStatement ps = dbManager.getConexao().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, inicio.toString());
            ps.setString(3, fim.toString());
            return mapear(ps.executeQuery());
        }
    }

    /**
     * Busca eventos cujo título contenha o termo informado (pesquisa parcial, case-insensitive).
     * Retorna no máximo 30 resultados, ordenados por data decrescente.
     *
     * @param usuarioId identificador do usuário
     * @param titulo    texto a buscar no título
     * @return lista de eventos correspondentes (pode estar vazia)
     * @throws SQLException se ocorrer erro na consulta
     */
    public List<Evento> buscarPorTitulo(int usuarioId, String titulo) throws SQLException {
        String sql = """
            SELECT * FROM evento
            WHERE usuario_id = ? AND titulo LIKE ?
            ORDER BY data DESC, hora_inicio
            LIMIT 30
            """;
        try (PreparedStatement ps = dbManager.getConexao().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, "%" + titulo + "%");
            return mapear(ps.executeQuery());
        }
    }

    /**
     * Retorna todos os eventos do usuário para a data de hoje, ordenados por hora de início.
     *
     * @param usuarioId identificador do usuário
     * @return lista de eventos de hoje (pode estar vazia)
     * @throws SQLException se ocorrer erro na consulta
     */
    public List<Evento> buscarHoje(int usuarioId) throws SQLException {
        String sql = "SELECT * FROM evento WHERE usuario_id = ? AND data = ? ORDER BY hora_inicio";
        try (PreparedStatement ps = dbManager.getConexao().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, LocalDate.now().toString());
            return mapear(ps.executeQuery());
        }
    }

    private List<Evento> mapear(ResultSet rs) throws SQLException {
        List<Evento> lista = new ArrayList<>();
        while (rs.next()) {
            Evento e = new Evento();
            e.setId(rs.getInt("id"));
            e.setUsuarioId(rs.getInt("usuario_id"));
            e.setTitulo(rs.getString("titulo"));
            e.setDescricao(rs.getString("descricao"));
            e.setData(LocalDate.parse(rs.getString("data")));
            String hi = rs.getString("hora_inicio");
            e.setHoraInicio(hi != null ? LocalTime.parse(hi, FMT_HORA) : null);
            String hf = rs.getString("hora_fim");
            e.setHoraFim(hf != null ? LocalTime.parse(hf, FMT_HORA) : null);
            String cr = rs.getString("criado_em");
            if (cr != null) e.setCriadoEm(LocalDateTime.parse(cr, FMT_DT));
            lista.add(e);
        }
        return lista;
    }
}
