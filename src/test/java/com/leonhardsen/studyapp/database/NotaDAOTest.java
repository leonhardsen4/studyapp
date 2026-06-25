package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.ItemArvore;
import com.leonhardsen.studyapp.model.Nota;
import com.leonhardsen.studyapp.model.TipoItem;
import com.leonhardsen.studyapp.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para {@link NotaDAO} usando banco SQLite em memória.
 *
 * @author StudyApp
 */
class NotaDAOTest {

    private NotaDAO notaDAO;
    private ItemArvoreDAO itemDAO;
    private int itemId;

    @BeforeEach
    void configurar() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        DatabaseManager.resetarParaTeste(conn);
        DatabaseManager db = DatabaseManager.getInstance();
        notaDAO = new NotaDAO(db);
        itemDAO = new ItemArvoreDAO(db);
        UsuarioDAO usuarioDAO = new UsuarioDAO(db);

        // Cadastra usuário e item para servir de base nos testes
        Usuario u = new Usuario();
        u.setNome("T"); u.setEmail("t@t.com"); u.setSenhaHash("$h$"); u.setTema("CLARO");
        usuarioDAO.inserir(u);

        ItemArvore item = new ItemArvore();
        item.setUsuarioId(u.getId());
        item.setPaiId(null);
        item.setNome("Nota Teste");
        item.setTipo(TipoItem.NOTA);
        item.setPosicao(0);
        itemDAO.inserir(item);
        itemId = item.getId();
    }

    @Test
    void inserir_atribuiId() throws SQLException {
        Nota nota = new Nota();
        nota.setItemId(itemId);
        nota.setConteudo("Conteúdo inicial");
        notaDAO.inserir(nota);
        assertTrue(nota.getId() > 0);
    }

    @Test
    void buscarPorItemId_retornaNotaCorreta() throws SQLException {
        Nota nota = new Nota();
        nota.setItemId(itemId);
        nota.setConteudo("# Título\n\nParágrafo.");
        notaDAO.inserir(nota);

        Nota encontrada = notaDAO.buscarPorItemId(itemId);
        assertNotNull(encontrada);
        assertEquals("# Título\n\nParágrafo.", encontrada.getConteudo());
        assertEquals(itemId, encontrada.getItemId());
    }

    @Test
    void buscarPorItemId_retornaNull_itemInexistente() throws SQLException {
        assertNull(notaDAO.buscarPorItemId(9999));
    }

    @Test
    void atualizarConteudo_persisteNovoCOnteudo() throws SQLException {
        Nota nota = new Nota();
        nota.setItemId(itemId);
        nota.setConteudo("Original");
        notaDAO.inserir(nota);

        notaDAO.atualizarConteudo(itemId, "Atualizado com **markdown**");
        assertEquals("Atualizado com **markdown**", notaDAO.buscarPorItemId(itemId).getConteudo());
    }

    @Test
    void inserir_conteudoVazioPermitido() throws SQLException {
        Nota nota = new Nota();
        nota.setItemId(itemId);
        nota.setConteudo("");
        notaDAO.inserir(nota);
        Nota encontrada = notaDAO.buscarPorItemId(itemId);
        assertNotNull(encontrada);
        assertEquals("", encontrada.getConteudo());
    }

    @Test
    void atualizarConteudo_suportaMarkdownLongo() throws SQLException {
        String conteudoLongo = "# Título\n\n".repeat(100) + "Fim";
        Nota nota = new Nota();
        nota.setItemId(itemId);
        nota.setConteudo("");
        notaDAO.inserir(nota);

        notaDAO.atualizarConteudo(itemId, conteudoLongo);
        assertEquals(conteudoLongo, notaDAO.buscarPorItemId(itemId).getConteudo());
    }
}
