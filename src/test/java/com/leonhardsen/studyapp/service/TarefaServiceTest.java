package com.leonhardsen.studyapp.service;

import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.database.UsuarioDAO;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para {@link TarefaService} usando banco SQLite em memória.
 */
class TarefaServiceTest {

    private TarefaService service;
    private int usuarioId;

    @BeforeEach
    void configurar() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        DatabaseManager.resetarParaTeste(conn);
        service = new TarefaService();

        UsuarioDAO usuarioDAO = new UsuarioDAO(DatabaseManager.getInstance());
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

    // ── Etiquetas ─────────────────────────────────────────────────────────────

    @Test
    void criarEtiqueta_retornaEtiquetaComId() throws Exception {
        Etiqueta e = service.criarEtiqueta(usuarioId, "Faculdade");
        assertTrue(e.getId() > 0);
        assertEquals("Faculdade", e.getNome());
    }

    @Test
    void criarEtiqueta_nomeVazio_lancaExcecao() {
        assertThrows(Exception.class, () -> service.criarEtiqueta(usuarioId, "  "));
    }

    @Test
    void listarEtiquetas_retornaEtiquetasCriadas() throws Exception {
        service.criarEtiqueta(usuarioId, "Prova");
        service.criarEtiqueta(usuarioId, "Lab");
        List<Etiqueta> lista = service.listarEtiquetas(usuarioId);
        assertEquals(2, lista.size());
    }

    @Test
    void excluirEtiqueta_removeSemCascata_quandoTarefaTemOutraEtiqueta() throws Exception {
        Etiqueta e1 = service.criarEtiqueta(usuarioId, "A");
        Etiqueta e2 = service.criarEtiqueta(usuarioId, "B");
        Tarefa t = tarefaFixture("Multi-etiqueta");
        service.criarTarefa(t, List.of(e1.getId(), e2.getId()));

        service.excluirEtiqueta(e1.getId());

        // tarefa ainda existe pois tem outra etiqueta
        List<Tarefa> tarefas = service.listarTarefas(usuarioId);
        assertEquals(1, tarefas.size());
        // etiqueta A foi removida
        assertEquals(1, service.listarEtiquetas(usuarioId).size());
    }

    @Test
    void excluirEtiqueta_removeTarefaExclusiva_emCascata() throws Exception {
        Etiqueta e = service.criarEtiqueta(usuarioId, "Única");
        Tarefa t = tarefaFixture("Exclusiva");
        service.criarTarefa(t, List.of(e.getId()));

        service.excluirEtiqueta(e.getId());

        assertTrue(service.listarTarefas(usuarioId).isEmpty());
        assertTrue(service.listarEtiquetas(usuarioId).isEmpty());
    }

    // ── Tarefas ───────────────────────────────────────────────────────────────

    @Test
    void criarTarefa_semEtiquetas_lancaExcecao() throws Exception {
        Etiqueta e = service.criarEtiqueta(usuarioId, "X");
        assertThrows(Exception.class, () -> service.criarTarefa(tarefaFixture("T"), List.of()));
    }

    @Test
    void criarTarefa_maisDeTreeEtiquetas_lancaExcecao() throws Exception {
        Etiqueta e1 = service.criarEtiqueta(usuarioId, "A");
        Etiqueta e2 = service.criarEtiqueta(usuarioId, "B");
        Etiqueta e3 = service.criarEtiqueta(usuarioId, "C");
        Etiqueta e4 = service.criarEtiqueta(usuarioId, "D");
        assertThrows(Exception.class,
                () -> service.criarTarefa(tarefaFixture("T"), List.of(e1.getId(), e2.getId(), e3.getId(), e4.getId())));
    }

    @Test
    void criarTarefa_tituloVazio_lancaExcecao() throws Exception {
        Etiqueta e = service.criarEtiqueta(usuarioId, "X");
        assertThrows(Exception.class,
                () -> service.criarTarefa(tarefaFixture("  "), List.of(e.getId())));
    }

    @Test
    void criarTarefa_persisteComEtiquetasVinculadas() throws Exception {
        Etiqueta e = service.criarEtiqueta(usuarioId, "Prova");
        Tarefa t = service.criarTarefa(tarefaFixture("Estudar"), List.of(e.getId()));

        assertTrue(t.getId() > 0);
        assertEquals(1, t.getEtiquetas().size());
        assertEquals("Prova", t.getEtiquetas().get(0).getNome());
    }

    @Test
    void excluirTarefa_removeDaLista() throws Exception {
        Etiqueta e = service.criarEtiqueta(usuarioId, "Lab");
        Tarefa t = service.criarTarefa(tarefaFixture("Para excluir"), List.of(e.getId()));
        service.excluirTarefa(t.getId());
        assertTrue(service.listarTarefas(usuarioId).isEmpty());
    }
}
