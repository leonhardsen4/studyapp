package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.Usuario;

import java.sql.*;

/**
 * Objeto de acesso a dados para a entidade {@link Usuario}.
 * Realiza todas as operações de leitura e escrita de usuários no banco de dados SQLite.
 *
 * @author StudyApp
 * @version 1.0
 */
public class UsuarioDAO {

    private final DatabaseManager dbManager;

    /**
     * Construtor que recebe a instância do gerenciador de banco de dados.
     *
     * @param dbManager gerenciador de conexão com o banco de dados
     */
    public UsuarioDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Insere um novo usuário no banco de dados.
     * O campo {@code id} do objeto é atualizado com o valor gerado automaticamente.
     *
     * @param usuario objeto com os dados do usuário a ser inserido
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void inserir(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuario (nome, email, senha_hash, tema) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getSenhaHash());
            stmt.setString(4, usuario.getTema() != null ? usuario.getTema() : "CLARO");
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Busca um usuário pelo seu endereço de e-mail.
     *
     * @param email endereço de e-mail a ser pesquisado
     * @return objeto {@link Usuario} encontrado, ou {@code null} se não existir
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public Usuario buscarPorEmail(String email) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE email = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    /**
     * Busca um usuário pelo seu identificador único.
     *
     * @param id identificador do usuário
     * @return objeto {@link Usuario} encontrado, ou {@code null} se não existir
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public Usuario buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    /**
     * Atualiza o endereço de e-mail de um usuário.
     *
     * @param usuarioId identificador do usuário
     * @param novoEmail novo endereço de e-mail
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void atualizarEmail(int usuarioId, String novoEmail) throws SQLException {
        String sql = "UPDATE usuario SET email = ? WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setString(1, novoEmail);
            stmt.setInt(2, usuarioId);
            stmt.executeUpdate();
        }
    }

    /**
     * Atualiza o hash de senha de um usuário.
     *
     * @param usuarioId    identificador do usuário
     * @param novaSenhaHash novo hash BCrypt da senha
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void atualizarSenha(int usuarioId, String novaSenhaHash) throws SQLException {
        String sql = "UPDATE usuario SET senha_hash = ? WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setString(1, novaSenhaHash);
            stmt.setInt(2, usuarioId);
            stmt.executeUpdate();
        }
    }

    /**
     * Atualiza a preferência de tema visual de um usuário.
     *
     * @param usuarioId identificador do usuário
     * @param tema      novo tema ("CLARO" ou "ESCURO")
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void atualizarTema(int usuarioId, String tema) throws SQLException {
        String sql = "UPDATE usuario SET tema = ? WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setString(1, tema);
            stmt.setInt(2, usuarioId);
            stmt.executeUpdate();
        }
    }

    /**
     * Exclui permanentemente um usuário e todos os seus dados (cascata via chave estrangeira).
     *
     * @param usuarioId identificador do usuário a ser excluído
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void excluir(int usuarioId) throws SQLException {
        String sql = "DELETE FROM usuario WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            stmt.executeUpdate();
        }
    }

    /**
     * Verifica se um endereço de e-mail já está cadastrado no sistema.
     *
     * @param email endereço de e-mail a verificar
     * @return {@code true} se o e-mail já existe, {@code false} caso contrário
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public boolean emailExiste(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario WHERE email = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Converte uma linha do ResultSet em um objeto {@link Usuario}.
     *
     * @param rs ResultSet posicionado na linha a ser mapeada
     * @return objeto Usuario preenchido com os dados da linha
     * @throws SQLException se ocorrer erro ao ler o ResultSet
     */
    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("senha_hash"),
                rs.getString("tema")
        );
    }
}
