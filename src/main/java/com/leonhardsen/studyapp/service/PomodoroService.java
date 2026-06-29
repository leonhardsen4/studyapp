package com.leonhardsen.studyapp.service;

import com.leonhardsen.studyapp.database.AssuntoDAO;
import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.database.DisciplinaDAO;
import com.leonhardsen.studyapp.database.SessaoPomodoroDAO;
import com.leonhardsen.studyapp.model.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço de negócio para o módulo Pomodoro.
 * Centraliza operações sobre {@link Disciplina}, {@link Assunto} e {@link SessaoPomodoro},
 * aplicando validações e coordenando os DAOs correspondentes.
 */
public class PomodoroService {

    private final DisciplinaDAO disciplinaDAO = new DisciplinaDAO(DatabaseManager.getInstance());
    private final AssuntoDAO assuntoDAO = new AssuntoDAO(DatabaseManager.getInstance());
    private final SessaoPomodoroDAO sessaoDAO = new SessaoPomodoroDAO(DatabaseManager.getInstance());

    // ── Disciplina ────────────────────────────────────────────────────────────

    /**
     * Cria uma nova disciplina para o usuário informado.
     *
     * @param usuarioId identificador do usuário
     * @param nome      nome da disciplina (não pode ser vazio)
     * @return disciplina criada com ID preenchido
     * @throws SQLException             se ocorrer erro de persistência
     * @throws IllegalArgumentException se o nome for vazio
     */
    public Disciplina criarDisciplina(int usuarioId, String nome) throws SQLException {
        nome = nome.trim();
        if (nome.isBlank()) throw new IllegalArgumentException("O nome da disciplina é obrigatório.");
        Disciplina d = new Disciplina(usuarioId, nome);
        int id = disciplinaDAO.inserir(d);
        d.setId(id);
        return d;
    }

    /**
     * Retorna todas as disciplinas arquivadas do usuário, ordenadas pelo nome.
     *
     * @param usuarioId identificador do usuário
     * @return lista de disciplinas arquivadas (pode estar vazia)
     * @throws SQLException se ocorrer erro de persistência
     */
    public List<Disciplina> buscarDisciplinasArquivadas(int usuarioId) throws SQLException {
        return disciplinaDAO.buscarArquivadas(usuarioId);
    }

    /**
     * Arquiva a disciplina, ocultando-a da visualização principal do Plano de Estudos.
     *
     * @param id identificador da disciplina
     * @throws SQLException se ocorrer erro de persistência
     */
    public void arquivarDisciplina(int id) throws SQLException {
        disciplinaDAO.arquivar(id);
    }

    /**
     * Desarquiva a disciplina, tornando-a visível novamente no Plano de Estudos.
     *
     * @param id identificador da disciplina
     * @throws SQLException se ocorrer erro de persistência
     */
    public void desarquivarDisciplina(int id) throws SQLException {
        disciplinaDAO.desarquivar(id);
    }

    /**
     * Renomeia uma disciplina existente.
     *
     * @param id       identificador da disciplina
     * @param novoNome novo nome da disciplina (não pode ser vazio)
     * @throws SQLException             se ocorrer erro de persistência
     * @throws IllegalArgumentException se o novo nome for vazio
     */
    public void renomearDisciplina(int id, String novoNome) throws SQLException {
        novoNome = novoNome.trim();
        if (novoNome.isBlank()) throw new IllegalArgumentException("O nome da disciplina é obrigatório.");
        Disciplina d = new Disciplina();
        d.setId(id);
        d.setNome(novoNome);
        disciplinaDAO.atualizar(d);
    }

    /**
     * Exclui uma disciplina e todos os seus assuntos vinculados.
     *
     * @param id identificador da disciplina a excluir
     * @return número de assuntos que existiam antes da exclusão
     * @throws SQLException se ocorrer erro de persistência
     */
    public int excluirDisciplina(int id) throws SQLException {
        int total = disciplinaDAO.contarAssuntos(id);
        disciplinaDAO.excluir(id);
        return total;
    }

    /**
     * Retorna todas as disciplinas do usuário, ordenadas pelo nome.
     *
     * @param usuarioId identificador do usuário
     * @return lista de disciplinas (pode estar vazia)
     * @throws SQLException se ocorrer erro de persistência
     */
    public List<Disciplina> buscarDisciplinas(int usuarioId) throws SQLException {
        return disciplinaDAO.buscarPorUsuario(usuarioId);
    }

    // ── Assunto ───────────────────────────────────────────────────────────────

    /**
     * Cria um novo assunto vinculado a uma disciplina.
     *
     * @param disciplinaId   identificador da disciplina pai
     * @param nome           nome do assunto (não pode ser vazio)
     * @param dificuldade    nível de dificuldade do assunto
     * @param sessoesMinimas número mínimo de sessões Pomodoro (mínimo 1)
     * @param dataLimite     data limite para o estudo, ou {@code null} se não houver
     * @return assunto criado com ID preenchido
     * @throws SQLException             se ocorrer erro de persistência
     * @throws IllegalArgumentException se o nome for vazio ou sessões mínimas &lt; 1
     */
    public Assunto criarAssunto(int disciplinaId, String nome, TipoDificuldade dificuldade,
                                int sessoesMinimas, LocalDate dataLimite) throws SQLException {
        nome = nome.trim();
        if (nome.isBlank()) throw new IllegalArgumentException("O nome do assunto é obrigatório.");
        if (sessoesMinimas < 1) throw new IllegalArgumentException("O mínimo de sessões deve ser pelo menos 1.");
        Assunto a = new Assunto(disciplinaId, nome, dificuldade, sessoesMinimas);
        a.setDataLimite(dataLimite);
        int id = assuntoDAO.inserir(a);
        a.setId(id);
        return a;
    }

    /**
     * Edita os dados de um assunto existente.
     *
     * @param id             identificador do assunto
     * @param nome           novo nome do assunto (não pode ser vazio)
     * @param dificuldade    novo nível de dificuldade
     * @param sessoesMinimas novo número mínimo de sessões (mínimo 1)
     * @param dataLimite     nova data limite, ou {@code null} para remover
     * @throws SQLException             se ocorrer erro de persistência
     * @throws IllegalArgumentException se o nome for vazio ou sessões mínimas &lt; 1
     */
    public void editarAssunto(int id, String nome, TipoDificuldade dificuldade,
                              int sessoesMinimas, LocalDate dataLimite) throws SQLException {
        nome = nome.trim();
        if (nome.isBlank()) throw new IllegalArgumentException("O nome do assunto é obrigatório.");
        if (sessoesMinimas < 1) throw new IllegalArgumentException("O mínimo de sessões deve ser pelo menos 1.");
        Assunto a = assuntoDAO.buscarPorId(id);
        if (a == null) return;
        a.setNome(nome);
        a.setDificuldade(dificuldade);
        a.setSessoesMinimas(sessoesMinimas);
        a.setDataLimite(dataLimite);
        assuntoDAO.atualizar(a);
    }

    /**
     * Exclui o assunto com o ID informado.
     *
     * @param id identificador do assunto a excluir
     * @throws SQLException se ocorrer erro de persistência
     */
    public void excluirAssunto(int id) throws SQLException {
        assuntoDAO.excluir(id);
    }

    /**
     * Retorna todos os assuntos de uma disciplina, ordenados pelo nome.
     *
     * @param disciplinaId identificador da disciplina
     * @return lista de assuntos (pode estar vazia)
     * @throws SQLException se ocorrer erro de persistência
     */
    public List<Assunto> buscarAssuntos(int disciplinaId) throws SQLException {
        return assuntoDAO.buscarPorDisciplina(disciplinaId);
    }

    /**
     * Incrementa em 1 o contador de sessões realizadas do assunto e atualiza seu status
     * para EM_ANDAMENTO caso ainda esteja PENDENTE.
     *
     * @param id identificador do assunto
     * @return assunto atualizado, ou {@code null} se não encontrado
     * @throws SQLException se ocorrer erro de persistência
     */
    public Assunto incrementarSessao(int id) throws SQLException {
        Assunto a = assuntoDAO.buscarPorId(id);
        if (a == null) return null;
        a.setSessoesRealizadas(a.getSessoesRealizadas() + 1);
        if (a.getStatus() == TipoStatusAssunto.PENDENTE) {
            a.setStatus(TipoStatusAssunto.EM_ANDAMENTO);
        }
        assuntoDAO.atualizar(a);
        return a;
    }

    /**
     * Ajusta manualmente o contador de sessões realizadas de um assunto e recalcula o status.
     *
     * @param id          identificador do assunto
     * @param novasSessoes novo valor de sessões realizadas (valores negativos são normalizados para 0)
     * @return assunto atualizado, ou {@code null} se não encontrado
     * @throws SQLException se ocorrer erro de persistência
     */
    public Assunto ajustarSessoes(int id, int novasSessoes) throws SQLException {
        if (novasSessoes < 0) novasSessoes = 0;
        Assunto a = assuntoDAO.buscarPorId(id);
        if (a == null) return null;
        a.setSessoesRealizadas(novasSessoes);
        if (a.getStatus() != TipoStatusAssunto.CONCLUIDO) {
            a.setStatus(novasSessoes > 0 ? TipoStatusAssunto.EM_ANDAMENTO : TipoStatusAssunto.PENDENTE);
        } else if (novasSessoes < a.getSessoesMinimas()) {
            a.setStatus(TipoStatusAssunto.EM_ANDAMENTO);
        }
        assuntoDAO.atualizar(a);
        return a;
    }

    /**
     * Marca um assunto como concluído, independentemente do número de sessões realizadas.
     *
     * @param id identificador do assunto
     * @throws SQLException se ocorrer erro de persistência
     */
    public void marcarConcluido(int id) throws SQLException {
        Assunto a = assuntoDAO.buscarPorId(id);
        if (a == null) return;
        a.setStatus(TipoStatusAssunto.CONCLUIDO);
        assuntoDAO.atualizar(a);
    }

    /**
     * Reabre um assunto concluído, ajustando o status para EM_ANDAMENTO ou PENDENTE
     * conforme o número de sessões realizadas.
     *
     * @param id identificador do assunto
     * @throws SQLException se ocorrer erro de persistência
     */
    public void reabrirAssunto(int id) throws SQLException {
        Assunto a = assuntoDAO.buscarPorId(id);
        if (a == null) return;
        a.setStatus(a.getSessoesRealizadas() > 0 ? TipoStatusAssunto.EM_ANDAMENTO : TipoStatusAssunto.PENDENTE);
        assuntoDAO.atualizar(a);
    }

    // ── Sessões ───────────────────────────────────────────────────────────────

    /**
     * Registra uma sessão de foco concluída no banco de dados.
     *
     * @param usuarioId       identificador do usuário
     * @param assuntoId       identificador do assunto estudado, ou {@code null} se nenhum
     * @param inicio          data/hora de início da sessão
     * @param fim             data/hora de conclusão da sessão
     * @param duracaoSegundos duração efetiva da sessão em segundos
     * @throws SQLException se ocorrer erro de persistência
     */
    public void registrarSessaoFoco(int usuarioId, Integer assuntoId,
                                    LocalDateTime inicio, LocalDateTime fim,
                                    int duracaoSegundos) throws SQLException {
        SessaoPomodoro s = new SessaoPomodoro(usuarioId, assuntoId, TipoSessao.FOCO,
                                              inicio, fim, duracaoSegundos);
        sessaoDAO.registrar(s);
    }

    /**
     * Retorna o número de sessões de foco concluídas pelo usuário no dia atual.
     *
     * @param usuarioId identificador do usuário
     * @return quantidade de sessões de foco de hoje
     * @throws SQLException se ocorrer erro de persistência
     */
    public int contarSessoesHoje(int usuarioId) throws SQLException {
        return sessaoDAO.contarSessoesHoje(usuarioId);
    }

    /**
     * Retorna a soma total (em segundos) de todas as sessões de foco concluídas hoje.
     *
     * @param usuarioId identificador do usuário
     * @return total de segundos de foco acumulados hoje
     * @throws SQLException se ocorrer erro de persistência
     */
    public int somarDuracaoHoje(int usuarioId) throws SQLException {
        return sessaoDAO.somarDuracaoHoje(usuarioId);
    }

    /**
     * Retorna a soma total (em segundos) de todas as sessões de foco realizadas para um assunto.
     *
     * @param assuntoId identificador do assunto
     * @return total de segundos de foco do assunto
     * @throws SQLException se ocorrer erro de persistência
     */
    public int somarDuracaoPorAssunto(int assuntoId) throws SQLException {
        return sessaoDAO.somarDuracaoPorAssunto(assuntoId);
    }

    /**
     * Retorna a soma total (em segundos) de todas as sessões de foco realizadas para todos os
     * assuntos de uma disciplina.
     *
     * @param disciplinaId identificador da disciplina
     * @return total de segundos de foco da disciplina
     * @throws SQLException se ocorrer erro de persistência
     */
    public int somarDuracaoPorDisciplina(int disciplinaId) throws SQLException {
        return sessaoDAO.somarDuracaoPorDisciplina(disciplinaId);
    }

    /**
     * Retorna um resumo das últimas sessões de foco do usuário, com assunto e disciplina vinculados.
     * Cada elemento é um array {@code [duracao_segundos, concluido_em, assunto_nome, disciplina_nome]};
     * {@code assunto_nome} e {@code disciplina_nome} podem ser {@code null}.
     *
     * @param usuarioId identificador do usuário
     * @param limite    número máximo de sessões a retornar
     * @return lista de arrays, da mais recente para a mais antiga
     * @throws SQLException se ocorrer erro de persistência
     */
    public List<String[]> buscarResumoSessoesRecentes(int usuarioId, int limite) throws SQLException {
        return sessaoDAO.buscarResumoRecentes(usuarioId, limite);
    }

    /**
     * Retorna o histórico completo de sessões de foco de um assunto.
     * Cada elemento é {@code [concluido_em, duracao_segundos]}.
     *
     * @param assuntoId identificador do assunto
     * @return lista de arrays com data e duração, da mais recente para a mais antiga
     * @throws SQLException se ocorrer erro de persistência
     */
    public List<String[]> buscarHistoricoPorAssunto(int assuntoId) throws SQLException {
        return sessaoDAO.buscarHistoricoPorAssunto(assuntoId);
    }

    /**
     * Retorna o histórico completo de sessões de foco de todos os assuntos de uma disciplina.
     * Cada elemento é {@code [concluido_em, duracao_segundos, assunto_nome]}.
     *
     * @param disciplinaId identificador da disciplina
     * @return lista de arrays com data, duração e nome do assunto, da mais recente para a mais antiga
     * @throws SQLException se ocorrer erro de persistência
     */
    public List<String[]> buscarHistoricoPorDisciplina(int disciplinaId) throws SQLException {
        return sessaoDAO.buscarHistoricoPorDisciplina(disciplinaId);
    }
}
