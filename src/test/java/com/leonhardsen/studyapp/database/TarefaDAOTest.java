package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.Etiqueta;
import com.leonhardsen.studyapp.model.Tarefa;
import com.leonhardsen.studyapp.model.TipoPrioridade;
import com.leonhardsen.studyapp.model.TipoStatus;
import com.leonhardsen.studyapp.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para {@link TarefaDAO} usando banco SQLite em memória.
 */
class TarefaDAOTest {

    private TarefaDAO tarefaDAO;
    private EtiquetaDAO etiquetaDAO;
    private int usuarioId;

    @BeforeEach
    void configurar() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        DatabaseManager.resetarParaTeste(conn);
        DatabaseManager db = DatabaseManager.getInstance();
        tarefaDAO   = new TarefaDAO(db);
        etiquetaDAO = new EtiquetaDAO(db);

        UsuarioDAO usuarioDAO = new UsuarioDAO(db);
        Usuario u = new Usuario();
        u.setNome("Tester");
        u.setEmail("tester@test.com");
        u.setSenhaHash("$hash$");
        u.setTema("CLARO");
        usuarioDAO.inserir(u);
        usuarioId = u.getId();
    }

    private Tarefa tarefaFixture(String titulo) {
        Tarefa t = new Tarefa();
        t.setUsuarioId(usuarioId);
        t.setTitulo(titulo);
        t.setPrioridade(TipoPrioridade.MEDIA);
        t.setStatus(TipoStatus.PENDENTE);
        return t;
    }

    @Test
    void inserir_retornaIdPositivo() throws SQLException {
        int id = tarefaDAO.inserir(tarefaFixture("Estudar Java"));
        assertTrue(id > 0);
    }

    @Test
    void buscarTodas_retornaListaVaziaParaUsuarioSemTarefas() throws SQLException {
        assertTrue(tarefaDAO.buscarTodas(usuarioId).isEmpty());
    }

    @Test
    void buscarTodas_retornaTarefaInserida() throws SQLException {
        tarefaDAO.inserir(tarefaFixture("Estudar JavaFX"));
        List<Tarefa> lista = tarefaDAO.buscarTodas(usuarioId);
        assertEquals(1, lista.size());
        assertEquals("Estudar JavaFX", lista.get(0).getTitulo());
    }

    @Test
    void excluir_removeTarefaDaLista() throws SQLException {
        int id = tarefaDAO.inserir(tarefaFixture("Temporária"));
        tarefaDAO.excluir(id);
        assertTrue(tarefaDAO.buscarTodas(usuarioId).isEmpty());
    }

    @Test
    void atualizar_persisteMudancas() throws SQLException {
        Tarefa t = tarefaFixture("Original");
        int id = tarefaDAO.inserir(t);
        t.setId(id);
        t.setTitulo("Atualizada");
        t.setPrioridade(TipoPrioridade.ALTA);
        tarefaDAO.atualizar(t);

        Tarefa salva = tarefaDAO.buscarTodas(usuarioId).get(0);
        assertEquals("Atualizada", salva.getTitulo());
        assertEquals(TipoPrioridade.ALTA, salva.getPrioridade());
    }

    @Test
    void vincularEtiqueta_etiquetaAparecNaTarefa() throws SQLException {
        int etId = etiquetaDAO.inserir(usuarioId, "Prova");
        Tarefa t = tarefaFixture("Com etiqueta");
        int tarefaId = tarefaDAO.inserir(t);
        tarefaDAO.vincularEtiqueta(tarefaId, etId);

        List<Etiqueta> etiquetas = tarefaDAO.buscarEtiquetasDaTarefa(tarefaId);
        assertEquals(1, etiquetas.size());
        assertEquals("Prova", etiquetas.get(0).getNome());
    }

    @Test
    void desvincularTodasEtiquetas_removeVinculos() throws SQLException {
        int etId = etiquetaDAO.inserir(usuarioId, "Lab");
        Tarefa t = tarefaFixture("Multi");
        int tarefaId = tarefaDAO.inserir(t);
        tarefaDAO.vincularEtiqueta(tarefaId, etId);
        tarefaDAO.desvincularTodasEtiquetas(tarefaId);

        assertTrue(tarefaDAO.buscarEtiquetasDaTarefa(tarefaId).isEmpty());
    }

    @Test
    void buscarComAlerta_retornaTarefaVencidaNaoConcluida() throws SQLException {
        Tarefa t = tarefaFixture("Vencida");
        t.setDataVencimento(LocalDate.now().minusDays(1));
        tarefaDAO.inserir(t);

        List<Tarefa> alertas = tarefaDAO.buscarComAlerta(usuarioId, 3);
        assertEquals(1, alertas.size());
    }

    @Test
    void buscarComAlerta_naoRetornaTarefaConcluida() throws SQLException {
        Tarefa t = tarefaFixture("Concluída");
        t.setDataVencimento(LocalDate.now().minusDays(1));
        t.setStatus(TipoStatus.CONCLUIDA);
        tarefaDAO.inserir(t);

        assertTrue(tarefaDAO.buscarComAlerta(usuarioId, 3).isEmpty());
    }

    @Test
    void buscarComAlerta_naoRetornaTarefaSemVencimento() throws SQLException {
        tarefaDAO.inserir(tarefaFixture("Sem data"));
        assertTrue(tarefaDAO.buscarComAlerta(usuarioId, 3).isEmpty());
    }

    @Test
    void buscarTodas_isolamentoPorUsuario() throws SQLException {
        UsuarioDAO usuarioDAO = new UsuarioDAO(DatabaseManager.getInstance());
        Usuario outro = new Usuario();
        outro.setNome("Outro");
        outro.setEmail("outro@test.com");
        outro.setSenhaHash("$hash$");
        outro.setTema("CLARO");
        usuarioDAO.inserir(outro);

        Tarefa doBanco = tarefaFixture("Do usuário 1");
        tarefaDAO.inserir(doBanco);

        assertTrue(tarefaDAO.buscarTodas(outro.getId()).isEmpty());
    }
}
