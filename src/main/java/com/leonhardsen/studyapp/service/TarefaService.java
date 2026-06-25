package com.leonhardsen.studyapp.service;

import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.database.EtiquetaDAO;
import com.leonhardsen.studyapp.database.TarefaDAO;
import com.leonhardsen.studyapp.model.Etiqueta;
import com.leonhardsen.studyapp.model.Tarefa;

import java.sql.SQLException;
import java.util.List;

public class TarefaService {

    private final TarefaDAO tarefaDAO;
    private final EtiquetaDAO etiquetaDAO;

    public TarefaService() {
        DatabaseManager db = DatabaseManager.getInstance();
        this.tarefaDAO   = new TarefaDAO(db);
        this.etiquetaDAO = new EtiquetaDAO(db);
    }

    // ── Etiquetas ──────────────────────────────────────────────────────────────

    public Etiqueta criarEtiqueta(int usuarioId, String nome) throws Exception {
        if (nome == null || nome.isBlank()) throw new Exception("O nome da etiqueta não pode ser vazio.");
        int id = etiquetaDAO.inserir(usuarioId, nome.trim());
        return etiquetaDAO.buscarPorId(id);
    }

    public void renomearEtiqueta(int id, String novoNome) throws Exception {
        if (novoNome == null || novoNome.isBlank()) throw new Exception("O nome não pode ser vazio.");
        etiquetaDAO.atualizarNome(id, novoNome.trim());
    }

    /**
     * Exclui a etiqueta. Tarefas que pertencem SOMENTE a esta etiqueta são
     * removidas em cascata antes da exclusão da etiqueta em si.
     */
    public void excluirEtiqueta(int etiquetaId) throws SQLException {
        List<Integer> exclusivas = etiquetaDAO.buscarIdsTarefasExclusivas(etiquetaId);
        for (int tarefaId : exclusivas) {
            tarefaDAO.excluir(tarefaId);
        }
        etiquetaDAO.excluir(etiquetaId);
    }

    public List<Etiqueta> listarEtiquetas(int usuarioId) throws SQLException {
        return etiquetaDAO.buscarTodas(usuarioId);
    }

    // ── Tarefas ───────────────────────────────────────────────────────────────

    public Tarefa criarTarefa(Tarefa tarefa, List<Integer> etiquetaIds) throws Exception {
        validarTarefa(tarefa, etiquetaIds);
        int id = tarefaDAO.inserir(tarefa);
        tarefa.setId(id);
        for (int eid : etiquetaIds) tarefaDAO.vincularEtiqueta(id, eid);
        tarefa.setEtiquetas(tarefaDAO.buscarEtiquetasDaTarefa(id));
        return tarefa;
    }

    public void atualizarTarefa(Tarefa tarefa, List<Integer> etiquetaIds) throws Exception {
        validarTarefa(tarefa, etiquetaIds);
        tarefaDAO.atualizar(tarefa);
        tarefaDAO.desvincularTodasEtiquetas(tarefa.getId());
        for (int eid : etiquetaIds) tarefaDAO.vincularEtiqueta(tarefa.getId(), eid);
        tarefa.setEtiquetas(tarefaDAO.buscarEtiquetasDaTarefa(tarefa.getId()));
    }

    public void excluirTarefa(int tarefaId) throws SQLException {
        tarefaDAO.excluir(tarefaId);
    }

    public List<Tarefa> listarTarefas(int usuarioId) throws SQLException {
        return tarefaDAO.buscarTodas(usuarioId);
    }

    /** Retorna tarefas não concluídas com deadline vencido ou nos próximos 3 dias. */
    public List<Tarefa> buscarTarefasComAlerta(int usuarioId) throws SQLException {
        return tarefaDAO.buscarComAlerta(usuarioId, 3);
    }

    private void validarTarefa(Tarefa t, List<Integer> etiquetaIds) throws Exception {
        if (t.getTitulo() == null || t.getTitulo().isBlank()) {
            throw new Exception("O título da tarefa é obrigatório.");
        }
        if (etiquetaIds == null || etiquetaIds.isEmpty()) {
            throw new Exception("A tarefa deve ter ao menos uma etiqueta.");
        }
        if (etiquetaIds.size() > 3) {
            throw new Exception("A tarefa pode ter no máximo 3 etiquetas.");
        }
    }
}
