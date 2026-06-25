package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.Etiqueta;
import com.leonhardsen.studyapp.model.Tarefa;
import com.leonhardsen.studyapp.model.TipoPrioridade;
import com.leonhardsen.studyapp.model.TipoStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TarefaDAO {

    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FMT_D  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DatabaseManager db;

    public TarefaDAO(DatabaseManager db) {
        this.db = db;
    }

    public int inserir(Tarefa t) throws SQLException {
        String sql = """
            INSERT INTO tarefa(usuario_id, titulo, anotacoes, prioridade, status, data_vencimento)
            VALUES(?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getUsuarioId());
            ps.setString(2, t.getTitulo().trim());
            ps.setString(3, t.getAnotacoes());
            ps.setString(4, t.getPrioridade().name());
            ps.setString(5, t.getStatus().name());
            if (t.getDataVencimento() != null) {
                ps.setString(6, t.getDataVencimento().format(FMT_D));
            } else {
                ps.setNull(6, Types.VARCHAR);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public void atualizar(Tarefa t) throws SQLException {
        String sql = """
            UPDATE tarefa
            SET titulo = ?, anotacoes = ?, prioridade = ?, status = ?,
                data_vencimento = ?, atualizado_em = datetime('now')
            WHERE id = ?
            """;
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setString(1, t.getTitulo().trim());
            ps.setString(2, t.getAnotacoes());
            ps.setString(3, t.getPrioridade().name());
            ps.setString(4, t.getStatus().name());
            if (t.getDataVencimento() != null) {
                ps.setString(5, t.getDataVencimento().format(FMT_D));
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            ps.setInt(6, t.getId());
            ps.executeUpdate();
        }
    }

    public void excluir(int tarefaId) throws SQLException {
        String sql = "DELETE FROM tarefa WHERE id = ?";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, tarefaId);
            ps.executeUpdate();
        }
    }

    public List<Tarefa> buscarTodas(int usuarioId) throws SQLException {
        String sql = """
            SELECT id, usuario_id, titulo, anotacoes, prioridade, status,
                   data_vencimento, criado_em, atualizado_em
            FROM tarefa WHERE usuario_id = ?
            ORDER BY atualizado_em DESC
            """;
        List<Tarefa> lista = new ArrayList<>();
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        // carrega etiquetas de cada tarefa
        for (Tarefa t : lista) {
            t.setEtiquetas(buscarEtiquetasDaTarefa(t.getId()));
        }
        return lista;
    }

    public List<Etiqueta> buscarEtiquetasDaTarefa(int tarefaId) throws SQLException {
        String sql = """
            SELECT e.id, e.usuario_id, e.nome, e.criado_em
            FROM etiqueta e
            JOIN tarefa_etiqueta te ON te.etiqueta_id = e.id
            WHERE te.tarefa_id = ?
            ORDER BY e.nome COLLATE NOCASE
            """;
        List<Etiqueta> lista = new ArrayList<>();
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, tarefaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Etiqueta(rs.getInt("id"), rs.getInt("usuario_id"),
                            rs.getString("nome"), rs.getString("criado_em")));
                }
            }
        }
        return lista;
    }

    public void vincularEtiqueta(int tarefaId, int etiquetaId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO tarefa_etiqueta(tarefa_id, etiqueta_id) VALUES(?, ?)";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, tarefaId);
            ps.setInt(2, etiquetaId);
            ps.executeUpdate();
        }
    }

    public void desvincularTodasEtiquetas(int tarefaId) throws SQLException {
        String sql = "DELETE FROM tarefa_etiqueta WHERE tarefa_id = ?";
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, tarefaId);
            ps.executeUpdate();
        }
    }

    /** Busca tarefas com data de vencimento dentro dos próximos {@code dias} dias ou já vencidas. */
    public List<Tarefa> buscarComAlerta(int usuarioId, int diasAntecedencia) throws SQLException {
        String sql = """
            SELECT id, usuario_id, titulo, anotacoes, prioridade, status,
                   data_vencimento, criado_em, atualizado_em
            FROM tarefa
            WHERE usuario_id = ?
              AND status != 'CONCLUIDA'
              AND data_vencimento IS NOT NULL
              AND date(data_vencimento) <= date('now', '+' || ? || ' days')
            ORDER BY data_vencimento ASC
            """;
        List<Tarefa> lista = new ArrayList<>();
        try (PreparedStatement ps = db.getConexao().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setInt(2, diasAntecedencia);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        for (Tarefa t : lista) t.setEtiquetas(buscarEtiquetasDaTarefa(t.getId()));
        return lista;
    }

    private Tarefa mapear(ResultSet rs) throws SQLException {
        Tarefa t = new Tarefa();
        t.setId(rs.getInt("id"));
        t.setUsuarioId(rs.getInt("usuario_id"));
        t.setTitulo(rs.getString("titulo"));
        t.setAnotacoes(rs.getString("anotacoes"));
        t.setPrioridade(TipoPrioridade.valueOf(rs.getString("prioridade")));
        t.setStatus(TipoStatus.valueOf(rs.getString("status")));
        String venc = rs.getString("data_vencimento");
        if (venc != null && !venc.isBlank()) t.setDataVencimento(LocalDate.parse(venc, FMT_D));
        String criado = rs.getString("criado_em");
        if (criado != null) t.setCriadoEm(LocalDateTime.parse(criado, FMT_DT));
        String atualizado = rs.getString("atualizado_em");
        if (atualizado != null) t.setAtualizadoEm(LocalDateTime.parse(atualizado, FMT_DT));
        return t;
    }
}
