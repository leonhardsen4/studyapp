package com.leonhardsen.studyapp.service;

import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.database.EventoDAO;
import com.leonhardsen.studyapp.model.Evento;

import java.sql.SQLException;
import java.util.List;

/**
 * Serviço de negócio para operações com {@link Evento}.
 * Aplica validações antes de delegar a persistência ao {@link EventoDAO}.
 */
public class EventoService {

    private final EventoDAO eventoDAO;

    /**
     * Cria o serviço inicializando o DAO com a instância singleton do banco de dados.
     */
    public EventoService() {
        this.eventoDAO = new EventoDAO(DatabaseManager.getInstance());
    }

    /**
     * Valida e persiste um novo evento, preenchendo o ID gerado no objeto.
     *
     * @param e evento a ser criado
     * @return o mesmo evento com o ID preenchido
     * @throws Exception se os dados forem inválidos ou ocorrer erro de persistência
     */
    public Evento criarEvento(Evento e) throws Exception {
        validar(e);
        int id = eventoDAO.inserir(e);
        e.setId(id);
        return e;
    }

    /**
     * Valida e atualiza os dados de um evento existente.
     *
     * @param e evento com os dados atualizados (deve ter ID válido)
     * @throws Exception se os dados forem inválidos ou ocorrer erro de persistência
     */
    public void atualizarEvento(Evento e) throws Exception {
        validar(e);
        eventoDAO.atualizar(e);
    }

    /**
     * Exclui o evento com o ID informado.
     *
     * @param id identificador do evento a excluir
     * @throws SQLException se ocorrer erro de persistência
     */
    public void excluirEvento(int id) throws SQLException {
        eventoDAO.excluir(id);
    }

    /**
     * Retorna todos os eventos de um usuário em um mês/ano específico.
     *
     * @param usuarioId identificador do usuário
     * @param ano       ano desejado
     * @param mes       mês desejado (1–12)
     * @return lista de eventos do período
     * @throws SQLException se ocorrer erro de persistência
     */
    public List<Evento> buscarPorMes(int usuarioId, int ano, int mes) throws SQLException {
        return eventoDAO.buscarPorMes(usuarioId, ano, mes);
    }

    /**
     * Retorna todos os eventos do usuário para a data de hoje.
     *
     * @param usuarioId identificador do usuário
     * @return lista de eventos de hoje
     * @throws SQLException se ocorrer erro de persistência
     */
    public List<Evento> buscarEventosHoje(int usuarioId) throws SQLException {
        return eventoDAO.buscarHoje(usuarioId);
    }

    /**
     * Busca eventos cujo título contenha o termo informado (pesquisa parcial).
     *
     * @param usuarioId identificador do usuário
     * @param titulo    texto a buscar no título
     * @return lista de eventos correspondentes
     * @throws SQLException se ocorrer erro de persistência
     */
    public List<Evento> buscarPorTitulo(int usuarioId, String titulo) throws SQLException {
        return eventoDAO.buscarPorTitulo(usuarioId, titulo);
    }

    private void validar(Evento e) throws Exception {
        if (e.getTitulo() == null || e.getTitulo().isBlank())
            throw new Exception("O título do evento é obrigatório.");
        if (e.getData() == null)
            throw new Exception("A data do evento é obrigatória.");
        if (e.getHoraFim() != null && e.getHoraInicio() == null)
            throw new Exception("Informe o horário de início ou remova o horário de fim.");
        if (e.getHoraInicio() != null && e.getHoraFim() != null
                && !e.getHoraFim().isAfter(e.getHoraInicio()))
            throw new Exception("O horário de fim deve ser posterior ao horário de início.");
    }
}
