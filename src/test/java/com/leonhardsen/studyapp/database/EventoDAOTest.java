package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.Evento;
import com.leonhardsen.studyapp.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para {@link EventoDAO} usando banco SQLite em memória.
 */
class EventoDAOTest {

    private EventoDAO eventoDAO;
    private int usuarioId;

    @BeforeEach
    void configurar() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        DatabaseManager.resetarParaTeste(conn);
        DatabaseManager db = DatabaseManager.getInstance();
        eventoDAO = new EventoDAO(db);

        UsuarioDAO usuarioDAO = new UsuarioDAO(db);
        Usuario u = new Usuario();
        u.setNome("Tester");
        u.setEmail("tester@test.com");
        u.setSenhaHash("$hash$");
        u.setTema("CLARO");
        usuarioDAO.inserir(u);
        usuarioId = u.getId();
    }

    private Evento eventoFixture(String titulo, LocalDate data) {
        Evento e = new Evento();
        e.setUsuarioId(usuarioId);
        e.setTitulo(titulo);
        e.setData(data);
        return e;
    }

    @Test
    void inserir_retornaIdPositivo() throws SQLException {
        int id = eventoDAO.inserir(eventoFixture("Prova", LocalDate.now()));
        assertTrue(id > 0);
    }

    @Test
    void buscarHoje_retornaEventoDoDiaAtual() throws SQLException {
        eventoDAO.inserir(eventoFixture("Reunião", LocalDate.now()));
        List<Evento> hoje = eventoDAO.buscarHoje(usuarioId);
        assertEquals(1, hoje.size());
        assertEquals("Reunião", hoje.get(0).getTitulo());
    }

    @Test
    void buscarHoje_naoRetornaEventoDeOutroDia() throws SQLException {
        eventoDAO.inserir(eventoFixture("Ontem", LocalDate.now().minusDays(1)));
        assertTrue(eventoDAO.buscarHoje(usuarioId).isEmpty());
    }

    @Test
    void buscarPorMes_retornaEventosDoMes() throws SQLException {
        LocalDate data = LocalDate.of(2026, 6, 15);
        eventoDAO.inserir(eventoFixture("Junho", data));
        List<Evento> lista = eventoDAO.buscarPorMes(usuarioId, 2026, 6);
        assertEquals(1, lista.size());
    }

    @Test
    void buscarPorMes_naoRetornaEventoDeOutroMes() throws SQLException {
        eventoDAO.inserir(eventoFixture("Julho", LocalDate.of(2026, 7, 1)));
        assertTrue(eventoDAO.buscarPorMes(usuarioId, 2026, 6).isEmpty());
    }

    @Test
    void atualizar_persisteMudancas() throws SQLException {
        Evento e = eventoFixture("Original", LocalDate.now());
        int id = eventoDAO.inserir(e);
        e.setId(id);
        e.setTitulo("Atualizado");
        e.setHoraInicio(LocalTime.of(10, 0));
        eventoDAO.atualizar(e);

        List<Evento> hoje = eventoDAO.buscarHoje(usuarioId);
        assertEquals("Atualizado", hoje.get(0).getTitulo());
        assertEquals(LocalTime.of(10, 0), hoje.get(0).getHoraInicio());
    }

    @Test
    void excluir_removeEventoDaLista() throws SQLException {
        int id = eventoDAO.inserir(eventoFixture("Temporário", LocalDate.now()));
        eventoDAO.excluir(id);
        assertTrue(eventoDAO.buscarHoje(usuarioId).isEmpty());
    }

    @Test
    void inserir_eventoComHoraFim_persisteCorretamente() throws SQLException {
        Evento e = eventoFixture("Com horário", LocalDate.now());
        e.setHoraInicio(LocalTime.of(9, 0));
        e.setHoraFim(LocalTime.of(11, 30));
        int id = eventoDAO.inserir(e);
        assertTrue(id > 0);

        Evento salvo = eventoDAO.buscarHoje(usuarioId).get(0);
        assertEquals(LocalTime.of(9, 0),  salvo.getHoraInicio());
        assertEquals(LocalTime.of(11, 30), salvo.getHoraFim());
    }

    @Test
    void buscarHoje_isolamentoPorUsuario() throws SQLException {
        UsuarioDAO usuarioDAO = new UsuarioDAO(DatabaseManager.getInstance());
        Usuario outro = new Usuario();
        outro.setNome("Outro");
        outro.setEmail("outro@test.com");
        outro.setSenhaHash("$hash$");
        outro.setTema("CLARO");
        usuarioDAO.inserir(outro);

        eventoDAO.inserir(eventoFixture("Meu evento", LocalDate.now()));
        assertTrue(eventoDAO.buscarHoje(outro.getId()).isEmpty());
    }
}
