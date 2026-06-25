package com.leonhardsen.studyapp.util;

import com.leonhardsen.studyapp.model.Usuario;

/**
 * Gerenciador de sessão do usuário autenticado.
 * Implementado como singleton, mantém em memória o objeto do usuário logado durante toda a execução.
 * Nenhuma informação de sessão é gravada em disco.
 *
 * @author StudyApp
 * @version 1.0
 */
public class SessionManager {

    private static SessionManager instancia;
    private Usuario usuarioLogado;

    /**
     * Construtor privado para impedir instanciação direta.
     */
    private SessionManager() {
    }

    /**
     * Retorna a instância única do gerenciador de sessão.
     *
     * @return instância do SessionManager
     */
    public static SessionManager getInstance() {
        if (instancia == null) {
            instancia = new SessionManager();
        }
        return instancia;
    }

    /**
     * Inicia a sessão com o usuário autenticado.
     *
     * @param usuario objeto do usuário que acabou de se autenticar
     */
    public void login(Usuario usuario) {
        this.usuarioLogado = usuario;
    }

    /**
     * Encerra a sessão atual, removendo o usuário da memória.
     */
    public void logout() {
        this.usuarioLogado = null;
    }

    /**
     * Retorna o usuário atualmente autenticado.
     *
     * @return objeto {@link Usuario} do usuário logado, ou {@code null} se não houver sessão ativa
     */
    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }

    /**
     * Verifica se há uma sessão ativa no momento.
     *
     * @return {@code true} se houver um usuário logado, {@code false} caso contrário
     */
    public boolean estaLogado() {
        return usuarioLogado != null;
    }
}
