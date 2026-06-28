package com.leonhardsen.studyapp.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gerenciador de conexão com o banco de dados SQLite.
 * Implementado como singleton, garante uma única conexão durante toda a execução da aplicação.
 * Ao inicializar, cria o arquivo de banco de dados e todas as tabelas necessárias caso não existam.
 *
 * @author StudyApp
 * @version 1.0
 */
public class DatabaseManager {

    private static DatabaseManager instancia;
    private Connection conexao;

    /** Caminho do diretório de dados da aplicação. */
    private static final String DIR_APP = System.getProperty("user.home") + "/.studyapp";

    /** Caminho completo do arquivo de banco de dados. */
    private static final String URL_BANCO = "jdbc:sqlite:" + DIR_APP + "/studyapp.db";

    /**
     * Construtor privado que cria o diretório de dados e inicializa a conexão.
     *
     * @throws RuntimeException se não for possível criar o banco de dados
     */
    private DatabaseManager() {
        new File(DIR_APP).mkdirs();
        new File(DIR_APP + "/pdfs").mkdirs();
        new File(DIR_APP + "/images").mkdirs();
        try {
            conexao = DriverManager.getConnection(URL_BANCO);
            configurarBanco();
            criarTabelas();
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao inicializar o banco de dados: " + e.getMessage(), e);
        }
    }

    /**
     * Construtor para uso exclusivo em testes.
     * Usa a conexão fornecida (ex: banco em memória) sem criar diretórios.
     *
     * @param conexaoExterna conexão JDBC já aberta
     * @throws SQLException se ocorrer erro ao configurar ou criar as tabelas
     */
    private DatabaseManager(Connection conexaoExterna) throws SQLException {
        this.conexao = conexaoExterna;
        configurarBanco();
        criarTabelas();
    }

    /**
     * Substitui o singleton por uma instância com conexão em memória — uso exclusivo em testes.
     *
     * @param conexaoExterna conexão JDBC (ex: {@code DriverManager.getConnection("jdbc:sqlite::memory:")})
     * @throws SQLException se ocorrer erro ao configurar o banco
     */
    public static void resetarParaTeste(Connection conexaoExterna) throws SQLException {
        instancia = new DatabaseManager(conexaoExterna);
    }

    /**
     * Retorna a instância única do gerenciador de banco de dados.
     * Cria a instância na primeira chamada (inicialização lazy).
     *
     * @return instância do DatabaseManager
     */
    public static DatabaseManager getInstance() {
        if (instancia == null) {
            instancia = new DatabaseManager();
        }
        return instancia;
    }

    /**
     * Retorna a conexão ativa com o banco de dados.
     * Reabre a conexão automaticamente se ela estiver fechada.
     *
     * @return conexão JDBC ativa
     * @throws SQLException se não for possível obter a conexão
     */
    public Connection getConexao() throws SQLException {
        if (conexao == null || conexao.isClosed()) {
            conexao = DriverManager.getConnection(URL_BANCO);
            configurarBanco();
        }
        return conexao;
    }

    /**
     * Aplica configurações iniciais de pragmas do SQLite:
     * ativa chaves estrangeiras e modo WAL para melhor performance.
     *
     * @throws SQLException se ocorrer erro ao executar os pragmas
     */
    private void configurarBanco() throws SQLException {
        try (Statement stmt = conexao.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");
        }
    }

    /**
     * Cria todas as tabelas do sistema caso ainda não existam.
     * Executado automaticamente na inicialização do banco.
     *
     * @throws SQLException se ocorrer erro ao criar as tabelas
     */
    private void criarTabelas() throws SQLException {
        try (Statement stmt = conexao.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuario (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome       TEXT    NOT NULL,
                    email      TEXT    NOT NULL UNIQUE,
                    senha_hash TEXT    NOT NULL,
                    tema       TEXT    NOT NULL DEFAULT 'CLARO',
                    criado_em  TEXT    NOT NULL DEFAULT (datetime('now'))
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS item_arvore (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id    INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                    pai_id        INTEGER REFERENCES item_arvore(id) ON DELETE CASCADE,
                    nome          TEXT    NOT NULL,
                    tipo          TEXT    NOT NULL CHECK(tipo IN ('CADERNO','NOTA','PDF')),
                    posicao       INTEGER NOT NULL DEFAULT 0,
                    criado_em     TEXT    NOT NULL DEFAULT (datetime('now')),
                    atualizado_em TEXT    NOT NULL DEFAULT (datetime('now'))
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS nota (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_id  INTEGER NOT NULL UNIQUE REFERENCES item_arvore(id) ON DELETE CASCADE,
                    conteudo TEXT    NOT NULL DEFAULT ''
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pdf_documento (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    item_id         INTEGER NOT NULL UNIQUE REFERENCES item_arvore(id) ON DELETE CASCADE,
                    caminho_arquivo TEXT    NOT NULL,
                    tamanho_bytes   INTEGER NOT NULL DEFAULT 0
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS etiqueta (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                    nome       TEXT    NOT NULL,
                    criado_em  TEXT    NOT NULL DEFAULT (datetime('now')),
                    UNIQUE(usuario_id, nome)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tarefa (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id      INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                    titulo          TEXT    NOT NULL,
                    anotacoes       TEXT    NOT NULL DEFAULT '',
                    prioridade      TEXT    NOT NULL DEFAULT 'MEDIA'
                                    CHECK(prioridade IN ('BAIXA','MEDIA','ALTA','URGENTE')),
                    status          TEXT    NOT NULL DEFAULT 'PENDENTE'
                                    CHECK(status IN ('PENDENTE','EM_ANDAMENTO','CONCLUIDA')),
                    data_vencimento TEXT,
                    criado_em       TEXT    NOT NULL DEFAULT (datetime('now')),
                    atualizado_em   TEXT    NOT NULL DEFAULT (datetime('now'))
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tarefa_etiqueta (
                    tarefa_id   INTEGER NOT NULL REFERENCES tarefa(id)   ON DELETE CASCADE,
                    etiqueta_id INTEGER NOT NULL REFERENCES etiqueta(id) ON DELETE CASCADE,
                    PRIMARY KEY (tarefa_id, etiqueta_id)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS evento (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id   INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                    titulo       TEXT    NOT NULL,
                    descricao    TEXT    NOT NULL DEFAULT '',
                    data         TEXT    NOT NULL,
                    hora_inicio  TEXT,
                    hora_fim     TEXT,
                    criado_em    TEXT    NOT NULL DEFAULT (datetime('now'))
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS disciplina (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                    nome       TEXT    NOT NULL,
                    criado_em  TEXT    NOT NULL DEFAULT (datetime('now')),
                    UNIQUE(usuario_id, nome)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS assunto (
                    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                    disciplina_id      INTEGER NOT NULL REFERENCES disciplina(id) ON DELETE CASCADE,
                    nome               TEXT    NOT NULL,
                    dificuldade        TEXT    NOT NULL DEFAULT 'MEDIO'
                                       CHECK(dificuldade IN ('FACIL','MEDIO','DIFICIL','MUITO_DIFICIL')),
                    sessoes_minimas    INTEGER NOT NULL DEFAULT 4,
                    sessoes_realizadas INTEGER NOT NULL DEFAULT 0,
                    status             TEXT    NOT NULL DEFAULT 'PENDENTE'
                                       CHECK(status IN ('PENDENTE','EM_ANDAMENTO','CONCLUIDO')),
                    data_limite        TEXT,
                    criado_em          TEXT    NOT NULL DEFAULT (datetime('now')),
                    atualizado_em      TEXT    NOT NULL DEFAULT (datetime('now'))
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sessao_pomodoro (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario_id       INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                    assunto_id       INTEGER REFERENCES assunto(id) ON DELETE SET NULL,
                    tipo             TEXT    NOT NULL CHECK(tipo IN ('FOCO','PAUSA_CURTA','PAUSA_LONGA')),
                    iniciado_em      TEXT    NOT NULL,
                    concluido_em     TEXT    NOT NULL,
                    duracao_segundos INTEGER NOT NULL
                )
                """);
        }
        migrarTabelas();
    }

    /**
     * Aplica migrações incrementais de esquema para bancos existentes.
     * Cada migração é idempotente: ignora erros de "column already exists".
     */
    private void migrarTabelas() {
        try (Statement stmt = conexao.createStatement()) {
            stmt.execute("ALTER TABLE disciplina ADD COLUMN arquivado INTEGER NOT NULL DEFAULT 0");
        } catch (SQLException ignored) { /* coluna já existe — ignorar */ }
    }

    /**
     * Retorna o caminho do diretório de dados da aplicação.
     *
     * @return caminho absoluto do diretório de dados
     */
    public static String getDirApp() {
        return DIR_APP;
    }
}
