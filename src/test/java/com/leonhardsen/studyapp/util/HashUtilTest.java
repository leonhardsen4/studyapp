package com.leonhardsen.studyapp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para {@link HashUtil}.
 *
 * @author StudyApp
 */
class HashUtilTest {

    @Test
    void gerarHash_retornaStringNaoNula() {
        String hash = HashUtil.gerarHash("minhasenha");
        assertNotNull(hash);
        assertFalse(hash.isBlank());
    }

    @Test
    void gerarHash_produzHashDiferenteDaSenha() {
        String senha = "minhasenha123";
        String hash = HashUtil.gerarHash(senha);
        assertNotEquals(senha, hash);
    }

    @Test
    void gerarHash_mesmaEntradaProduzeHashesDiferentes() {
        // BCrypt usa salt aleatório; dois hashes da mesma senha devem ser diferentes
        String hash1 = HashUtil.gerarHash("igual");
        String hash2 = HashUtil.gerarHash("igual");
        assertNotEquals(hash1, hash2);
    }

    @Test
    void verificar_senhaCorretaRetornaTrue() {
        String senha = "senha@Valida1";
        String hash = HashUtil.gerarHash(senha);
        assertTrue(HashUtil.verificar(senha, hash));
    }

    @Test
    void verificar_senhaErradaRetornaFalse() {
        String hash = HashUtil.gerarHash("correta");
        assertFalse(HashUtil.verificar("errada", hash));
    }

    @Test
    void verificar_senhaVaziaRetornaFalse() {
        String hash = HashUtil.gerarHash("alguma");
        assertFalse(HashUtil.verificar("", hash));
    }

    @Test
    void verificar_sensivelaCase() {
        String hash = HashUtil.gerarHash("Senha");
        assertFalse(HashUtil.verificar("senha", hash));
        assertFalse(HashUtil.verificar("SENHA", hash));
        assertTrue(HashUtil.verificar("Senha", hash));
    }
}
