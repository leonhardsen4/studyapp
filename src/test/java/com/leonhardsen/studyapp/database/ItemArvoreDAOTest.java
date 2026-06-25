package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.ItemArvore;
import com.leonhardsen.studyapp.model.TipoItem;
import com.leonhardsen.studyapp.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para {@link ItemArvoreDAO} usando banco SQLite em memória.
 *
 * @author StudyApp
 */
class ItemArvoreDAOTest {

    private ItemArvoreDAO itemDAO;
    private UsuarioDAO usuarioDAO;
    private int usuarioId;

    @BeforeEach
    void configurar() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        DatabaseManager.resetarParaTeste(conn);
        DatabaseManager db = DatabaseManager.getInstance();
        itemDAO = new ItemArvoreDAO(db);
        usuarioDAO = new UsuarioDAO(db);

        // Cria usuário base para os testes
        Usuario u = new Usuario();
        u.setNome("Tester");
        u.setEmail("tester@test.com");
        u.setSenhaHash("$hash$");
        u.setTema("CLARO");
        usuarioDAO.inserir(u);
        usuarioId = u.getId();
    }

    private ItemArvore caderno(String nome, Integer paiId) {
        ItemArvore item = new ItemArvore();
        item.setUsuarioId(usuarioId);
        item.setPaiId(paiId);
        item.setNome(nome);
        item.setTipo(TipoItem.CADERNO);
        item.setPosicao(0);
        return item;
    }

    private ItemArvore nota(String nome, int paiId) {
        ItemArvore item = new ItemArvore();
        item.setUsuarioId(usuarioId);
        item.setPaiId(paiId);
        item.setNome(nome);
        item.setTipo(TipoItem.NOTA);
        item.setPosicao(0);
        return item;
    }

    @Test
    void inserir_atribuiId() throws SQLException {
        ItemArvore c = caderno("Caderno 1", null);
        itemDAO.inserir(c);
        assertTrue(c.getId() > 0);
    }

    @Test
    void buscarTodos_retornaItensdoUsuario() throws SQLException {
        itemDAO.inserir(caderno("C1", null));
        itemDAO.inserir(caderno("C2", null));
        List<ItemArvore> itens = itemDAO.buscarTodos(usuarioId);
        assertEquals(2, itens.size());
    }

    @Test
    void buscarTodos_naoRetornaItensDeOutroUsuario() throws SQLException {
        // Cria segundo usuário
        Usuario outro = new Usuario();
        outro.setNome("Outro"); outro.setEmail("outro@test.com");
        outro.setSenhaHash("$h$"); outro.setTema("CLARO");
        usuarioDAO.inserir(outro);

        ItemArvore itemOutro = caderno("Do outro", null);
        itemOutro.setUsuarioId(outro.getId());
        itemDAO.inserir(itemOutro);

        itemDAO.inserir(caderno("Do principal", null));

        List<ItemArvore> itens = itemDAO.buscarTodos(usuarioId);
        assertEquals(1, itens.size());
        assertEquals("Do principal", itens.get(0).getNome());
    }

    @Test
    void buscarCadernos_retornaApenasCadernos() throws SQLException {
        ItemArvore c = caderno("Caderno", null);
        itemDAO.inserir(c);
        ItemArvore n = nota("Nota", c.getId());
        itemDAO.inserir(n);

        List<ItemArvore> cadernos = itemDAO.buscarCadernos(usuarioId);
        assertEquals(1, cadernos.size());
        assertEquals(TipoItem.CADERNO, cadernos.get(0).getTipo());
    }

    @Test
    void atualizarNome_alteraNome() throws SQLException {
        ItemArvore c = caderno("Antigo", null);
        itemDAO.inserir(c);
        itemDAO.atualizarNome(c.getId(), "Novo Nome");

        List<ItemArvore> itens = itemDAO.buscarTodos(usuarioId);
        assertEquals("Novo Nome", itens.get(0).getNome());
    }

    @Test
    void mover_alteraPaiId() throws SQLException {
        ItemArvore pai1 = caderno("Pai 1", null);
        ItemArvore pai2 = caderno("Pai 2", null);
        itemDAO.inserir(pai1);
        itemDAO.inserir(pai2);

        ItemArvore filho = nota("Nota", pai1.getId());
        itemDAO.inserir(filho);

        itemDAO.mover(filho.getId(), pai2.getId());

        // Após mover, o filho deve aparecer somente uma vez
        List<ItemArvore> todos = itemDAO.buscarTodos(usuarioId);
        ItemArvore filhoAtualizado = todos.stream()
                .filter(i -> i.getId() == filho.getId())
                .findFirst().orElseThrow();
        assertEquals(pai2.getId(), filhoAtualizado.getPaiId());
    }

    @Test
    void excluir_removeItem() throws SQLException {
        ItemArvore c = caderno("Excluível", null);
        itemDAO.inserir(c);
        itemDAO.excluir(c.getId());
        assertTrue(itemDAO.buscarTodos(usuarioId).isEmpty());
    }

    @Test
    void excluir_cascataFilhos() throws SQLException {
        ItemArvore pai = caderno("Pai", null);
        itemDAO.inserir(pai);
        ItemArvore filho = nota("Filho", pai.getId());
        itemDAO.inserir(filho);

        itemDAO.excluir(pai.getId());
        assertTrue(itemDAO.buscarTodos(usuarioId).isEmpty());
    }

    @Test
    void contarDescendentes_zero_semFilhos() throws SQLException {
        ItemArvore c = caderno("Folha", null);
        itemDAO.inserir(c);
        assertEquals(0, itemDAO.contarDescendentes(c.getId(), usuarioId));
    }

    @Test
    void contarDescendentes_contaRecursivamente() throws SQLException {
        ItemArvore raiz = caderno("Raiz", null);
        itemDAO.inserir(raiz);
        ItemArvore sub = caderno("Sub", raiz.getId());
        itemDAO.inserir(sub);
        ItemArvore folha = nota("Folha", sub.getId());
        itemDAO.inserir(folha);

        assertEquals(2, itemDAO.contarDescendentes(raiz.getId(), usuarioId));
    }

    @Test
    void excluirPorUsuario_removeTodasItens() throws SQLException {
        itemDAO.inserir(caderno("C1", null));
        itemDAO.inserir(caderno("C2", null));
        itemDAO.excluirPorUsuario(usuarioId);
        assertTrue(itemDAO.buscarTodos(usuarioId).isEmpty());
    }
}
