package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para {@link UsuarioDAO} usando banco SQLite em memória.
 *
 * @author StudyApp
 */
class UsuarioDAOTest {

    private UsuarioDAO dao;

    @BeforeEach
    void configurar() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        DatabaseManager.resetarParaTeste(conn);
        dao = new UsuarioDAO(DatabaseManager.getInstance());
    }

    private Usuario usuarioFixture(String email) {
        Usuario u = new Usuario();
        u.setNome("Teste");
        u.setEmail(email);
        u.setSenhaHash("$hash$");
        u.setTema("CLARO");
        return u;
    }

    @Test
    void inserir_atribuiIdAoObjeto() throws SQLException {
        Usuario u = usuarioFixture("a@b.com");
        dao.inserir(u);
        assertTrue(u.getId() > 0);
    }

    @Test
    void buscarPorEmail_encontraUsuarioCadastrado() throws SQLException {
        Usuario u = usuarioFixture("joao@email.com");
        dao.inserir(u);
        Usuario encontrado = dao.buscarPorEmail("joao@email.com");
        assertNotNull(encontrado);
        assertEquals("joao@email.com", encontrado.getEmail());
        assertEquals("Teste", encontrado.getNome());
    }

    @Test
    void buscarPorEmail_retornaNull_emailInexistente() throws SQLException {
        assertNull(dao.buscarPorEmail("inexistente@email.com"));
    }

    @Test
    void buscarPorId_encontraUsuarioCadastrado() throws SQLException {
        Usuario u = usuarioFixture("maria@email.com");
        dao.inserir(u);
        Usuario encontrado = dao.buscarPorId(u.getId());
        assertNotNull(encontrado);
        assertEquals(u.getId(), encontrado.getId());
        assertEquals("CLARO", encontrado.getTema());
    }

    @Test
    void buscarPorId_retornaNull_idInexistente() throws SQLException {
        assertNull(dao.buscarPorId(9999));
    }

    @Test
    void atualizarEmail_alteraEmailNoBanco() throws SQLException {
        Usuario u = usuarioFixture("antigo@email.com");
        dao.inserir(u);
        dao.atualizarEmail(u.getId(), "novo@email.com");
        assertEquals("novo@email.com", dao.buscarPorId(u.getId()).getEmail());
    }

    @Test
    void atualizarSenha_alteraHashNoBanco() throws SQLException {
        Usuario u = usuarioFixture("user@test.com");
        dao.inserir(u);
        dao.atualizarSenha(u.getId(), "$novo_hash$");
        assertEquals("$novo_hash$", dao.buscarPorId(u.getId()).getSenhaHash());
    }

    @Test
    void atualizarTema_alteraTema() throws SQLException {
        Usuario u = usuarioFixture("tema@test.com");
        dao.inserir(u);
        dao.atualizarTema(u.getId(), "ESCURO");
        assertEquals("ESCURO", dao.buscarPorId(u.getId()).getTema());
    }

    @Test
    void emailExiste_retornaTrue_emailCadastrado() throws SQLException {
        dao.inserir(usuarioFixture("existe@email.com"));
        assertTrue(dao.emailExiste("existe@email.com"));
    }

    @Test
    void emailExiste_retornaFalse_emailNaoExiste() throws SQLException {
        assertFalse(dao.emailExiste("naoexiste@email.com"));
    }

    @Test
    void excluir_removeUsuarioDoBanco() throws SQLException {
        Usuario u = usuarioFixture("excluir@email.com");
        dao.inserir(u);
        dao.excluir(u.getId());
        assertNull(dao.buscarPorId(u.getId()));
    }

    @Test
    void emailUnico_erroAoInserirEmailDuplicado() throws SQLException {
        dao.inserir(usuarioFixture("dup@email.com"));
        Usuario dup = usuarioFixture("dup@email.com");
        assertThrows(SQLException.class, () -> dao.inserir(dup));
    }
}
