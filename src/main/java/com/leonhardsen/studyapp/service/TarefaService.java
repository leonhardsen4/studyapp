package com.leonhardsen.studyapp.service;

import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.database.EtiquetaDAO;
import com.leonhardsen.studyapp.database.TarefaDAO;
import com.leonhardsen.studyapp.model.Etiqueta;
import com.leonhardsen.studyapp.model.Tarefa;

import java.sql.SQLException;
import java.util.List;

/**
 * Serviço de negócio para operações com {@link Tarefa} e {@link Etiqueta}.
 * Centraliza validações e coordena os DAOs de tarefas e etiquetas.
 */
public class TarefaService {

    private final TarefaDAO tarefaDAO;
    private final EtiquetaDAO etiquetaDAO;

    /**
     * Cria o serviço inicializando os DAOs com a instância singleton do banco de dados.
     */
    public TarefaService() {
        DatabaseManager db = DatabaseManager.getInstance();
        this.tarefaDAO   = new TarefaDAO(db);
        this.etiquetaDAO = new EtiquetaDAO(db);
    }

    // ── Etiquetas ──────────────────────────────────────────────────────────────

    /**
     * Cria uma nova etiqueta para o usuário informado.
     *
     * @param usuarioId identificador do usuário
     * @param nome      nome da etiqueta (não pode ser vazio)
     * @return etiqueta criada com ID preenchido
     * @throws Exception se o nome for vazio ou ocorrer erro de persistência
     */
    public Etiqueta criarEtiqueta(int usuarioId, String nome) throws Exception {
        if (nome == null || nome.isBlank()) throw new Exception("O nome da etiqueta não pode ser vazio.");
        int id = etiquetaDAO.inserir(usuarioId, nome.trim());
        return etiquetaDAO.buscarPorId(id);
    }

    /**
     * Renomeia uma etiqueta existente.
     *
     * @param id       identificador da etiqueta
     * @param novoNome novo nome (não pode ser vazio)
     * @throws Exception se o nome for vazio ou ocorrer erro de persistência
     */
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

    /**
     * Retorna todas as etiquetas de um usuário, ordenadas pelo nome.
     *
     * @param usuarioId identificador do usuário
     * @return lista de etiquetas (pode estar vazia)
     * @throws SQLException se ocorrer erro de persistência
     */
    public List<Etiqueta> listarEtiquetas(int usuarioId) throws SQLException {
        return etiquetaDAO.buscarTodas(usuarioId);
    }

    // ── Tarefas ───────────────────────────────────────────────────────────────

    /**
     * Valida e persiste uma nova tarefa, vinculando as etiquetas informadas.
     *
     * @param tarefa      tarefa a ser criada (sem ID definido)
     * @param etiquetaIds lista de IDs das etiquetas (1 a 3, não pode ser vazia)
     * @return tarefa criada com ID e etiquetas preenchidos
     * @throws Exception se a validação falhar ou ocorrer erro de persistência
     */
    public Tarefa criarTarefa(Tarefa tarefa, List<Integer> etiquetaIds) throws Exception {
        validarTarefa(tarefa, etiquetaIds);
        int id = tarefaDAO.inserir(tarefa);
        tarefa.setId(id);
        for (int eid : etiquetaIds) tarefaDAO.vincularEtiqueta(id, eid);
        tarefa.setEtiquetas(tarefaDAO.buscarEtiquetasDaTarefa(id));
        return tarefa;
    }

    /**
     * Valida e atualiza uma tarefa existente, redefinindo as etiquetas vinculadas.
     *
     * @param tarefa      tarefa com os dados atualizados (deve ter ID válido)
     * @param etiquetaIds nova lista de IDs das etiquetas (1 a 3, não pode ser vazia)
     * @throws Exception se a validação falhar ou ocorrer erro de persistência
     */
    public void atualizarTarefa(Tarefa tarefa, List<Integer> etiquetaIds) throws Exception {
        validarTarefa(tarefa, etiquetaIds);
        tarefaDAO.atualizar(tarefa);
        tarefaDAO.desvincularTodasEtiquetas(tarefa.getId());
        for (int eid : etiquetaIds) tarefaDAO.vincularEtiqueta(tarefa.getId(), eid);
        tarefa.setEtiquetas(tarefaDAO.buscarEtiquetasDaTarefa(tarefa.getId()));
    }

    /**
     * Exclui a tarefa com o ID informado.
     *
     * @param tarefaId identificador da tarefa a excluir
     * @throws SQLException se ocorrer erro de persistência
     */
    public void excluirTarefa(int tarefaId) throws SQLException {
        tarefaDAO.excluir(tarefaId);
    }

    /**
     * Retorna todas as tarefas do usuário com etiquetas carregadas.
     *
     * @param usuarioId identificador do usuário
     * @return lista de tarefas (pode estar vazia)
     * @throws SQLException se ocorrer erro de persistência
     */
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
