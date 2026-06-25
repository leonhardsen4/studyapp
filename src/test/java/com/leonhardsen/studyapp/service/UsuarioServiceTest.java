package com.leonhardsen.studyapp.service;

import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.model.Usuario;
import com.leonhardsen.studyapp.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração para {@link UsuarioService} usando banco SQLite em memória.
 * Cobre autenticação, cadastro, alteração de e-mail/senha e exclusão de conta.
 *
 * @author StudyApp
 */
class UsuarioServiceTest {

    private UsuarioService service;

    @BeforeEach
    void configurar() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        DatabaseManager.resetarParaTeste(conn);
        service = new UsuarioService();
    }

    // ── Cadastro ────────────────────────────────────────────────────

    @Test
    void cadastrar_sucesso_retornaUsuarioComId() throws SQLException {
        Usuario u = service.cadastrar("Ana", "ana@email.com", "senha123");
        assertTrue(u.getId() > 0);
        assertEquals("ana@email.com", u.getEmail());
    }

    @Test
    void cadastrar_emailNormalizadoParaMinusculas() throws SQLException {
        service.cadastrar("Bob", "BOB@EMAIL.COM", "senha123");
        // Deve ser possível autenticar com o email em minúsculas
        assertDoesNotThrow(() -> service.autenticar("bob@email.com", "senha123"));
    }

    @Test
    void cadastrar_senhaArmazenadaComoHash() throws SQLException {
        String senhaPlana = "minhaSenha";
        Usuario u = service.cadastrar("Carlos", "carlos@email.com", senhaPlana);
        assertNotEquals(senhaPlana, u.getSenhaHash());
        assertTrue(HashUtil.verificar(senhaPlana, u.getSenhaHash()));
    }

    @Test
    void cadastrar_temaPadraoEhClaro() throws SQLException {
        Usuario u = service.cadastrar("Diana", "diana@email.com", "senha123");
        assertEquals("CLARO", u.getTema());
    }

    @Test
    void cadastrar_erroEmailDuplicado() throws SQLException {
        service.cadastrar("E1", "mesmo@email.com", "senha123");
        assertThrows(IllegalArgumentException.class,
                () -> service.cadastrar("E2", "mesmo@email.com", "outrasenha"));
    }

    @Test
    void cadastrar_erroNomeEmBranco() {
        assertThrows(IllegalArgumentException.class,
                () -> service.cadastrar("", "f@email.com", "senha123"));
    }

    @Test
    void cadastrar_erroEmailEmBranco() {
        assertThrows(IllegalArgumentException.class,
                () -> service.cadastrar("G", "", "senha123"));
    }

    @Test
    void cadastrar_erroSenhaEmBranco() {
        assertThrows(IllegalArgumentException.class,
                () -> service.cadastrar("H", "h@email.com", ""));
    }

    // ── Autenticação ─────────────────────────────────────────────────

    @Test
    void autenticar_sucesso_retornaUsuario() throws SQLException {
        service.cadastrar("Ivan", "ivan@email.com", "senha456");
        Usuario logado = service.autenticar("ivan@email.com", "senha456");
        assertNotNull(logado);
        assertEquals("ivan@email.com", logado.getEmail());
    }

    @Test
    void autenticar_emailComMaiusculas_sucesso() throws SQLException {
        service.cadastrar("Julia", "julia@email.com", "senha123");
        // Email digitado com maiúsculas deve funcionar
        assertDoesNotThrow(() -> service.autenticar("JULIA@EMAIL.COM", "senha123"));
    }

    @Test
    void autenticar_erroEmailInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> service.autenticar("inexistente@email.com", "qualquer"));
    }

    @Test
    void autenticar_erroSenhaErrada() throws SQLException {
        service.cadastrar("Karen", "karen@email.com", "correta");
        assertThrows(IllegalArgumentException.class,
                () -> service.autenticar("karen@email.com", "errada"));
    }

    @Test
    void autenticar_erroSenhaVazia() throws SQLException {
        service.cadastrar("Leo", "leo@email.com", "senha");
        assertThrows(IllegalArgumentException.class,
                () -> service.autenticar("leo@email.com", ""));
    }

    // ── Alterar email ─────────────────────────────────────────────────

    @Test
    void alterarEmail_sucesso() throws SQLException {
        Usuario u = service.cadastrar("Mario", "mario@email.com", "senha");
        service.alterarEmail(u.getId(), "senha", "novo@email.com");
        // Deve ser possível autenticar com o novo email
        assertDoesNotThrow(() -> service.autenticar("novo@email.com", "senha"));
    }

    @Test
    void alterarEmail_erroSenhaErrada() throws SQLException {
        Usuario u = service.cadastrar("Nadia", "nadia@email.com", "correta");
        assertThrows(IllegalArgumentException.class,
                () -> service.alterarEmail(u.getId(), "errada", "outro@email.com"));
    }

    @Test
    void alterarEmail_erroEmailJaEmUso() throws SQLException {
        Usuario u1 = service.cadastrar("Oscar", "oscar@email.com", "s1");
        service.cadastrar("Paulo", "paulo@email.com", "s2");
        assertThrows(IllegalArgumentException.class,
                () -> service.alterarEmail(u1.getId(), "s1", "paulo@email.com"));
    }

    // ── Alterar senha ─────────────────────────────────────────────────

    @Test
    void alterarSenha_sucesso() throws SQLException {
        Usuario u = service.cadastrar("Quin", "quin@email.com", "velha");
        service.alterarSenha(u.getId(), "velha", "nova123");
        assertDoesNotThrow(() -> service.autenticar("quin@email.com", "nova123"));
    }

    @Test
    void alterarSenha_erroSenhaAtualIncorreta() throws SQLException {
        Usuario u = service.cadastrar("Rosa", "rosa@email.com", "correta");
        assertThrows(IllegalArgumentException.class,
                () -> service.alterarSenha(u.getId(), "errada", "nova123"));
    }

    @Test
    void alterarSenha_senhaAntigaNaoFunciona() throws SQLException {
        Usuario u = service.cadastrar("Sara", "sara@email.com", "antiga");
        service.alterarSenha(u.getId(), "antiga", "nova456");
        assertThrows(IllegalArgumentException.class,
                () -> service.autenticar("sara@email.com", "antiga"));
    }

    // ── Excluir conta ─────────────────────────────────────────────────

    @Test
    void excluirConta_sucesso_usuarioNaoEncontraveis() throws SQLException {
        Usuario u = service.cadastrar("Tiago", "tiago@email.com", "senha");
        service.excluirConta(u.getId(), "senha");
        assertThrows(IllegalArgumentException.class,
                () -> service.autenticar("tiago@email.com", "senha"));
    }

    @Test
    void excluirConta_erroSenhaIncorreta() throws SQLException {
        Usuario u = service.cadastrar("Ursula", "ursula@email.com", "correta");
        assertThrows(IllegalArgumentException.class,
                () -> service.excluirConta(u.getId(), "errada"));
        // Usuário deve ainda existir
        assertDoesNotThrow(() -> service.autenticar("ursula@email.com", "correta"));
    }

    @Test
    void excluirConta_removeCadernosPadrao() throws SQLException {
        // Ao cadastrar, é criado "Meus Cadernos" — deve ser removido junto com a conta
        Usuario u = service.cadastrar("Vera", "vera@email.com", "senha");
        service.excluirConta(u.getId(), "senha");
        // Se não lançar exceção, os itens foram removidos antes do usuário (sem violação FK)
    }

    // ── Atualizar tema ─────────────────────────────────────────────────

    @Test
    void atualizarTema_persisteTema() throws SQLException {
        Usuario u = service.cadastrar("Xavier", "xavier@email.com", "senha");
        service.atualizarTema(u.getId(), "ESCURO");
        Usuario logado = service.autenticar("xavier@email.com", "senha");
        assertEquals("ESCURO", logado.getTema());
    }
}
