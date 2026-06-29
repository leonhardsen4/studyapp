package com.leonhardsen.studyapp.service;

import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.database.UsuarioDAO;
import com.leonhardsen.studyapp.model.ItemArvore;
import com.leonhardsen.studyapp.model.TipoItem;
import com.leonhardsen.studyapp.model.Usuario;
import com.leonhardsen.studyapp.util.HashUtil;

import java.sql.SQLException;

/**
 * Serviço responsável pelas operações de negócio relacionadas ao usuário.
 * Encapsula autenticação, cadastro, alteração de dados e exclusão de conta,
 * coordenando a criação do caderno padrão ao registrar um novo usuário.
 *
 * @author StudyApp
 * @version 1.0
 */
public class UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final ArquivoService arquivoService;

    /**
     * Construtor que inicializa os DAOs e serviços necessários.
     */
    public UsuarioService() {
        DatabaseManager db = DatabaseManager.getInstance();
        this.usuarioDAO = new UsuarioDAO(db);
        this.arquivoService = new ArquivoService();
    }

    /**
     * Autentica um usuário com base no e-mail e senha fornecidos.
     * Verifica o hash BCrypt da senha antes de autorizar o acesso.
     *
     * @param email e-mail do usuário cadastrado
     * @param senha senha em texto puro fornecida pelo usuário
     * @return objeto {@link Usuario} autenticado se as credenciais forem válidas
     * @throws IllegalArgumentException se o e-mail não existir ou a senha estiver incorreta
     * @throws SQLException se ocorrer erro de acesso ao banco de dados
     */
    public Usuario autenticar(String email, String senha) throws SQLException {
        Usuario usuario = usuarioDAO.buscarPorEmail(email.trim().toLowerCase());
        if (usuario == null || !HashUtil.verificar(senha, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("E-mail ou senha incorretos.");
        }
        return usuario;
    }

    /**
     * Cadastra um novo usuário no sistema e cria automaticamente um caderno padrão para ele.
     *
     * @param nome  nome completo do novo usuário
     * @param email endereço de e-mail único
     * @param senha senha em texto puro (será armazenada como hash BCrypt)
     * @return objeto {@link Usuario} recém-criado com o id gerado pelo banco
     * @throws IllegalArgumentException se o e-mail já estiver cadastrado ou os campos estiverem em branco
     * @throws SQLException se ocorrer erro de acesso ao banco de dados
     */
    public Usuario cadastrar(String nome, String email, String senha) throws SQLException {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("O nome não pode estar em branco.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("O e-mail não pode estar em branco.");
        if (senha == null || senha.isBlank()) throw new IllegalArgumentException("A senha não pode estar em branco.");
        if (usuarioDAO.emailExiste(email)) throw new IllegalArgumentException("Este e-mail já está cadastrado.");

        Usuario usuario = new Usuario();
        usuario.setNome(nome.trim());
        usuario.setEmail(email.trim().toLowerCase());
        usuario.setSenhaHash(HashUtil.gerarHash(senha));
        usuario.setTema("CLARO");
        usuarioDAO.inserir(usuario);

        // Cria caderno padrão para o novo usuário
        ItemArvore cadernoPadrao = new ItemArvore();
        cadernoPadrao.setUsuarioId(usuario.getId());
        cadernoPadrao.setPaiId(null);
        cadernoPadrao.setNome("Meus Cadernos");
        cadernoPadrao.setTipo(TipoItem.CADERNO);
        cadernoPadrao.setPosicao(0);
        arquivoService.criarItem(cadernoPadrao);

        return usuario;
    }

    /**
     * Altera o endereço de e-mail do usuário autenticado.
     * Requer a senha atual para confirmar a operação.
     *
     * @param usuarioId identificador do usuário
     * @param senhaAtual senha atual em texto puro para confirmação
     * @param novoEmail  novo endereço de e-mail
     * @throws IllegalArgumentException se a senha estiver incorreta, o e-mail for inválido ou já existir
     * @throws SQLException se ocorrer erro de acesso ao banco de dados
     */
    public void alterarEmail(int usuarioId, String senhaAtual, String novoEmail) throws SQLException {
        Usuario usuario = usuarioDAO.buscarPorId(usuarioId);
        if (!HashUtil.verificar(senhaAtual, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        if (novoEmail == null || novoEmail.isBlank()) {
            throw new IllegalArgumentException("O novo e-mail não pode estar em branco.");
        }
        String emailNormalizado = novoEmail.trim().toLowerCase();
        if (!emailNormalizado.equals(usuario.getEmail()) && usuarioDAO.emailExiste(emailNormalizado)) {
            throw new IllegalArgumentException("Este e-mail já está sendo usado por outra conta.");
        }
        usuarioDAO.atualizarEmail(usuarioId, emailNormalizado);
    }

    /**
     * Altera a senha do usuário autenticado.
     * Requer a senha atual para confirmar a operação.
     *
     * @param usuarioId   identificador do usuário
     * @param senhaAtual  senha atual em texto puro para confirmação
     * @param novaSenha   nova senha em texto puro
     * @throws IllegalArgumentException se a senha atual estiver incorreta ou a nova senha estiver em branco
     * @throws SQLException se ocorrer erro de acesso ao banco de dados
     */
    public void alterarSenha(int usuarioId, String senhaAtual, String novaSenha) throws SQLException {
        Usuario usuario = usuarioDAO.buscarPorId(usuarioId);
        if (!HashUtil.verificar(senhaAtual, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new IllegalArgumentException("A nova senha não pode estar em branco.");
        }
        usuarioDAO.atualizarSenha(usuarioId, HashUtil.gerarHash(novaSenha));
    }

    /**
     * Exclui permanentemente a conta do usuário e todos os seus dados.
     * Requer a senha atual para confirmar a operação.
     *
     * @param usuarioId  identificador do usuário
     * @param senhaAtual senha atual em texto puro para confirmação
     * @throws IllegalArgumentException se a senha atual estiver incorreta
     * @throws SQLException se ocorrer erro de acesso ao banco de dados
     */
    public void excluirConta(int usuarioId, String senhaAtual) throws SQLException {
        Usuario usuario = usuarioDAO.buscarPorId(usuarioId);
        if (!HashUtil.verificar(senhaAtual, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Senha incorreta. A conta não foi excluída.");
        }
        // Remove itens antes do usuário para contornar ausência de CASCADE em bancos legados
        arquivoService.excluirTodosItens(usuarioId);
        usuarioDAO.excluir(usuarioId);
    }

    /**
     * Busca um usuário pelo endereço de e-mail (normalizado para minúsculas).
     *
     * @param email endereço de e-mail a pesquisar
     * @return objeto {@link Usuario} encontrado, ou {@code null} se não existir
     * @throws SQLException se ocorrer erro de acesso ao banco de dados
     */
    public Usuario buscarPorEmail(String email) throws SQLException {
        return usuarioDAO.buscarPorEmail(email.trim().toLowerCase());
    }

    /**
     * Redefine a senha de um usuário diretamente (sem confirmar a senha atual).
     * Usado exclusivamente no fluxo de recuperação de senha por e-mail.
     *
     * @param usuarioId    identificador do usuário
     * @param novoHashSenha novo hash BCrypt da senha temporária
     * @throws SQLException se ocorrer erro de acesso ao banco de dados
     */
    public void redefinirSenha(int usuarioId, String novoHashSenha) throws SQLException {
        usuarioDAO.atualizarSenha(usuarioId, novoHashSenha);
    }

    /**
     * Atualiza a preferência de tema visual do usuário no banco de dados.
     *
     * @param usuarioId identificador do usuário
     * @param tema      novo tema ("CLARO" ou "ESCURO")
     * @throws SQLException se ocorrer erro de acesso ao banco de dados
     */
    public void atualizarTema(int usuarioId, String tema) throws SQLException {
        usuarioDAO.atualizarTema(usuarioId, tema);
    }
}
