package com.leonhardsen.studyapp.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utilitário para hash e verificação de senhas usando o algoritmo BCrypt.
 * Utiliza fator de custo 12, que oferece boa segurança com tempo aceitável para aplicações desktop.
 *
 * @author StudyApp
 * @version 1.0
 */
public class HashUtil {

    /** Fator de custo do BCrypt (quanto maior, mais seguro e mais lento). */
    private static final int CUSTO_BCRYPT = 12;

    /**
     * Construtor privado — classe utilitária, não deve ser instanciada.
     */
    private HashUtil() {
    }

    /**
     * Gera o hash BCrypt de uma senha em texto puro.
     *
     * @param senha senha em texto puro a ser transformada em hash
     * @return string com o hash BCrypt da senha
     */
    public static String gerarHash(String senha) {
        return BCrypt.withDefaults().hashToString(CUSTO_BCRYPT, senha.toCharArray());
    }

    /**
     * Verifica se uma senha em texto puro corresponde a um hash BCrypt armazenado.
     *
     * @param senha     senha em texto puro fornecida pelo usuário
     * @param hashSalvo hash BCrypt armazenado no banco de dados
     * @return {@code true} se a senha corresponde ao hash, {@code false} caso contrário
     */
    public static boolean verificar(String senha, String hashSalvo) {
        BCrypt.Result resultado = BCrypt.verifyer().verify(senha.toCharArray(), hashSalvo);
        return resultado.verified;
    }
}
