package com.leonhardsen.studyapp.service;

import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.database.EventoDAO;
import com.leonhardsen.studyapp.model.Evento;

import java.sql.SQLException;
import java.util.List;

public class EventoService {

    private final EventoDAO eventoDAO;

    public EventoService() {
        this.eventoDAO = new EventoDAO(DatabaseManager.getInstance());
    }

    public Evento criarEvento(Evento e) throws Exception {
        validar(e);
        int id = eventoDAO.inserir(e);
        e.setId(id);
        return e;
    }

    public void atualizarEvento(Evento e) throws Exception {
        validar(e);
        eventoDAO.atualizar(e);
    }

    public void excluirEvento(int id) throws SQLException {
        eventoDAO.excluir(id);
    }

    public List<Evento> buscarPorMes(int usuarioId, int ano, int mes) throws SQLException {
        return eventoDAO.buscarPorMes(usuarioId, ano, mes);
    }

    public List<Evento> buscarEventosHoje(int usuarioId) throws SQLException {
        return eventoDAO.buscarHoje(usuarioId);
    }

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
